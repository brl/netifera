/*
 * Copyright 2009 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.jboss.netty.handler.execution;

import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelState;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.util.ObjectSizeEstimator;
import org.jboss.netty.util.internal.ConcurrentIdentityWeakKeyHashMap;

/**
 * A {@link MemoryAwareThreadPoolExecutor} which maintains the
 * {@link ChannelEvent} order for the same {@link Channel}.
 * <p>
 * {@link OrderedMemoryAwareThreadPoolExecutor} executes the queued task in the
 * same thread if an existing thread is running a task associated with the same
 * {@link Channel}.  This behavior is to make sure the events from the same
 * {@link Channel} are handled in a correct order.  A different thread might be
 * chosen only when the task queue of the {@link Channel} is empty.
 * <p>
 * Although {@link OrderedMemoryAwareThreadPoolExecutor} guarantees the order
 * of {@link ChannelEvent}s.  It does not guarantee that the invocation will be
 * made by the same thread for the same channel, but it does guarantee that
 * the invocation will be made sequentially for the events of the same channel.
 * For example, the events can be processed as depicted below:
 *
 * <pre>
 *           -----------------------------------&gt; Timeline -----------------------------------&gt;
 *
 * Thread X: --- Channel A (Event 1) --.   .-- Channel B (Event 2) --- Channel B (Event 3) ---&gt;
 *                                      \ /
 *                                       X
 *                                      / \
 * Thread Y: --- Channel B (Event 1) --'   '-- Channel A (Event 2) --- Channel A (Event 3) ---&gt;
 * </pre>
 *
 * Please note that the events of different channels are independent from each
 * other.  That is, an event of Channel B will not be blocked by an event of
 * Channel A and vice versa, unless the thread pool is exhausted.
 * <p>
 * If you want the events associated with the same channel to be executed
 * simultaneously, please use {@link MemoryAwareThreadPoolExecutor} instead.
 *
 * <h3>Using a different key other than {@link Channel} to maintain event order</h3>
 * <p>
 * {@link OrderedMemoryAwareThreadPoolExecutor} uses a {@link Channel} as a key
 * that is used for maintaining the event execution order, as explained in the
 * previous section.  Alternatively, you can extend it to change its behavior.
 * For example, you can change the key to the remote IP of the peer:
 *
 * <pre>
 * public class RemoteAddressBasedOMATPE extends OrderedMemoryAwareThreadPoolExecutor {
 *
 *     ... Constructors ...
 *
 *     protected ConcurrentMap&lt;Object, Executor&gt; new ChildExecutorMap() {
 *         // The default implementation returns a special ConcurrentMap that
 *         // uses identity comparison only (see {@link IdentityHashMap}).
 *         // Because SocketAddress does not work with identity comparison,
 *         // we need to employ more generic implementation.
 *         return new ConcurrentHashMap&lt;Object, Executor&gt;
 *     }
 *
 *     protected Object getChildExecutorKey(ChannelEvent e) {
 *         // Use the IP of the remote peer as a key.
 *         return ((InetSocketAddress) e.getChannel().getRemoteAddress()).getAddress();
 *     }
 *
 *     // Make public so that you can call from anywhere.
 *     public boolean removeChildExecutor(Object key) {
 *         super.removeChildExecutor(key);
 *     }
 * }
 * </pre>
 *
 * Please be very careful of memory leak of the child executor map.  You must
 * call {@link #removeChildExecutor(Object)} when the life cycle of the key
 * ends (e.g. all connections from the same IP were closed.)  Also, please
 * keep in mind that the key can appear again after calling {@link #removeChildExecutor(Object)}
 * (e.g. a new connection could come in from the same old IP after removal.)
 * If in doubt, prune the old unused or stall keys from the child executor map
 * periodically:
 *
 * <pre>
 * RemoteAddressBasedOMATPE executor = ...;
 *
 * on every 3 seconds:
 *
 *   for (Iterator&lt;Object&gt; i = executor.getChildExecutorKeySet().iterator; i.hasNext();) {
 *       InetAddress ip = (InetAddress) i.next();
 *       if (there is no active connection from 'ip' now &&
 *           there has been no incoming connection from 'ip' for last 10 minutes) {
 *           i.remove();
 *       }
 *   }
 * </pre>
 *
 * If the expected maximum number of keys is small and deterministic, you could
 * use a weak key map such as <a href="http://viewvc.jboss.org/cgi-bin/viewvc.cgi/jbosscache/experimental/jsr166/src/jsr166y/ConcurrentWeakHashMap.java?view=markup">ConcurrentWeakHashMap</a>
 * or synchronized {@link WeakHashMap} instead of managing the life cycle of the
 * keys by yourself.
 *
 * @author The Netty Project (netty-dev@lists.jboss.org)
 * @author Trustin Lee (trustin@gmail.com)
 * @author David M. Lloyd (david.lloyd@redhat.com)
 *
 * @version $Rev: 1783 $, $Date: 2009-10-14 14:46:40 +0900 (수, 14 10 2009) $
 *
 * @apiviz.landmark
 */
public class OrderedMemoryAwareThreadPoolExecutor extends
        MemoryAwareThreadPoolExecutor {

    private final ConcurrentMap<Object, Executor> childExecutors = newChildExecutorMap();

    /**
     * Creates a new instance.
     *
     * @param corePoolSize          the maximum number of active threads
     * @param maxChannelMemorySize  the maximum total size of the queued events per channel.
     *                              Specify {@code 0} to disable.
     * @param maxTotalMemorySize    the maximum total size of the queued events for this pool
     *                              Specify {@code 0} to disable.
     */
    public OrderedMemoryAwareThreadPoolExecutor(
            int corePoolSize, long maxChannelMemorySize, long maxTotalMemorySize) {
        super(corePoolSize, maxChannelMemorySize, maxTotalMemorySize);
    }

    /**
     * Creates a new instance.
     *
     * @param corePoolSize          the maximum number of active threads
     * @param maxChannelMemorySize  the maximum total size of the queued events per channel.
     *                              Specify {@code 0} to disable.
     * @param maxTotalMemorySize    the maximum total size of the queued events for this pool
     *                              Specify {@code 0} to disable.
     * @param keepAliveTime         the amount of time for an inactive thread to shut itself down
     * @param unit                  the {@link TimeUnit} of {@code keepAliveTime}
     */
    public OrderedMemoryAwareThreadPoolExecutor(
            int corePoolSize, long maxChannelMemorySize, long maxTotalMemorySize,
            long keepAliveTime, TimeUnit unit) {
        super(corePoolSize, maxChannelMemorySize, maxTotalMemorySize,
                keepAliveTime, unit);
    }

    /**
     * Creates a new instance.
     *
     * @param corePoolSize          the maximum number of active threads
     * @param maxChannelMemorySize  the maximum total size of the queued events per channel.
     *                              Specify {@code 0} to disable.
     * @param maxTotalMemorySize    the maximum total size of the queued events for this pool
     *                              Specify {@code 0} to disable.
     * @param keepAliveTime         the amount of time for an inactive thread to shut itself down
     * @param unit                  the {@link TimeUnit} of {@code keepAliveTime}
     * @param threadFactory         the {@link ThreadFactory} of this pool
     */
    public OrderedMemoryAwareThreadPoolExecutor(
            int corePoolSize, long maxChannelMemorySize, long maxTotalMemorySize,
            long keepAliveTime, TimeUnit unit, ThreadFactory threadFactory) {
        super(corePoolSize, maxChannelMemorySize, maxTotalMemorySize,
                keepAliveTime, unit, threadFactory);
    }

    /**
     * Creates a new instance.
     *
     * @param corePoolSize          the maximum number of active threads
     * @param maxChannelMemorySize  the maximum total size of the queued events per channel.
     *                              Specify {@code 0} to disable.
     * @param maxTotalMemorySize    the maximum total size of the queued events for this pool
     *                              Specify {@code 0} to disable.
     * @param keepAliveTime         the amount of time for an inactive thread to shut itself down
     * @param unit                  the {@link TimeUnit} of {@code keepAliveTime}
     * @param threadFactory         the {@link ThreadFactory} of this pool
     * @param objectSizeEstimator   the {@link ObjectSizeEstimator} of this pool
     */
    public OrderedMemoryAwareThreadPoolExecutor(
            int corePoolSize, long maxChannelMemorySize, long maxTotalMemorySize,
            long keepAliveTime, TimeUnit unit,
            ObjectSizeEstimator objectSizeEstimator, ThreadFactory threadFactory) {
        super(corePoolSize, maxChannelMemorySize, maxTotalMemorySize,
                keepAliveTime, unit, objectSizeEstimator, threadFactory);
    }

    protected ConcurrentMap<Object, Executor> newChildExecutorMap() {
        return new ConcurrentIdentityWeakKeyHashMap<Object, Executor>();
    }

    protected Object getChildExecutorKey(ChannelEvent e) {
        return e.getChannel();
    }

    protected Set<Object> getChildExecutorKeySet() {
        return childExecutors.keySet();
    }

    protected boolean removeChildExecutor(Object key) {
        return childExecutors.remove(key) != null;
    }

    /**
     * Executes the specified task concurrently while maintaining the event
     * order.
     */
    @Override
    protected void doExecute(Runnable task) {
        if (!(task instanceof ChannelEventRunnable)) {
            doUnorderedExecute(task);
        } else {
            ChannelEventRunnable r = (ChannelEventRunnable) task;
            getChildExecutor(r.getEvent()).execute(task);
        }
    }

    private Executor getChildExecutor(ChannelEvent e) {
        Object key = getChildExecutorKey(e);
        Executor executor = childExecutors.get(key);
        if (executor == null) {
            executor = new ChildExecutor();
            Executor oldExecutor = childExecutors.putIfAbsent(key, executor);
            if (oldExecutor != null) {
                executor = oldExecutor;
            }
        }

        // Remove the entry when the channel closes.
        if (e instanceof ChannelStateEvent) {
            Channel channel = e.getChannel();
            ChannelStateEvent se = (ChannelStateEvent) e;
            if (se.getState() == ChannelState.OPEN &&
                !channel.isOpen()) {
                childExecutors.remove(channel);
            }
        }
        return executor;
    }

    @Override
    protected boolean shouldCount(Runnable task) {
        if (task instanceof ChildExecutor) {
            return false;
        }

        return super.shouldCount(task);
    }

    void onAfterExecute(Runnable r, Throwable t) {
        afterExecute(r, t);
    }

    private final class ChildExecutor implements Executor, Runnable {
        private final LinkedList<Runnable> tasks = new LinkedList<Runnable>();

        ChildExecutor() {
            super();
        }

        public void execute(Runnable command) {
            boolean needsExecution;
            synchronized (tasks) {
                needsExecution = tasks.isEmpty();
                tasks.add(command);
            }

            if (needsExecution) {
                doUnorderedExecute(this);
            }
        }

        public void run() {
            Thread thread = Thread.currentThread();
            for (;;) {
                final Runnable task;
                synchronized (tasks) {
                    task = tasks.getFirst();
                }

                boolean ran = false;
                beforeExecute(thread, task);
                try {
                    task.run();
                    ran = true;
                    onAfterExecute(task, null);
                } catch (RuntimeException e) {
                    if (!ran) {
                        onAfterExecute(task, e);
                    }
                    throw e;
                } finally {
                    synchronized (tasks) {
                        tasks.removeFirst();
                        if (tasks.isEmpty()) {
                            break;
                        }
                    }
                }
            }
        }
    }
}
