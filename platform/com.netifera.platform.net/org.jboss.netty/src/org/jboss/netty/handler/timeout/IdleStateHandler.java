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
package org.jboss.netty.handler.timeout;

import static org.jboss.netty.channel.Channels.*;

import java.util.concurrent.TimeUnit;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.LifeCycleAwareChannelHandler;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.WriteCompletionEvent;
import org.jboss.netty.util.ExternalResourceReleasable;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timeout;
import org.jboss.netty.util.Timer;
import org.jboss.netty.util.TimerTask;

/**
 * Triggers an {@link IdleStateEvent} when a {@link Channel} has not performed
 * read, write, or both operation for a while.
 *
 * <h3>Supported idle states</h3>
 * <table border="1">
 * <tr>
 * <th>Property</th><th>Meaning</th>
 * </tr>
 * <tr>
 * <td>{@code readerIdleTime}</td>
 * <td>an {@link IdleStateEvent} whose state is {@link IdleState#READER_IDLE}
 *     will be triggered when no read was performed for the specified period of
 *     time.  Specify {@code 0} to disable.</td>
 * </tr>
 * <tr>
 * <td>{@code writerIdleTime}</td>
 * <td>an {@link IdleStateEvent} whose state is {@link IdleState#WRITER_IDLE}
 *     will be triggered when no write was performed for the specified period of
 *     time.  Specify {@code 0} to disable.</td>
 * </tr>
 * <tr>
 * <td>{@code allIdleTime}</td>
 * <td>an {@link IdleStateEvent} whose state is {@link IdleState#ALL_IDLE}
 *     will be triggered when neither read nor write was performed for the
 *     specified period of time.  Specify {@code 0} to disable.</td>
 * </tr>
 * </table>
 *
 * <pre>
 * // An example that sends a ping message when there is no traffic
 * // (either inbound or outbound) for 30 seconds.
 * ChannelPipeline p = ...;
 * Timer timer = new HashedWheelTimer();
 * p.addLast("timeout", new IdleStateHandler(timer, 30, 30, 0));
 * p.addLast("handler", new MyHandler());
 *
 * // Handler should handle the IdleStateEvent triggered by IdleStateHandler.
 * public class MyHandler extends IdleStateAwareChannelHandler {
 *     public void channelIdle(ChannelHandlerContext ctx, IdleStateEvent e) {
 *         ctx.getChannel().write(new PingMessage());
 *     }
 * }
 *
 * // To shut down, call {@link #releaseExternalResources()} or {@link Timer#stop()}.
 * </pre>
 *
 * @author The Netty Project (netty-dev@lists.jboss.org)
 * @author Trustin Lee (trustin@gmail.com)
 * @version $Rev: 1783 $, $Date: 2009-10-14 14:46:40 +0900 (수, 14 10 2009) $
 *
 * @see ReadTimeoutHandler
 * @see WriteTimeoutHandler
 *
 * @apiviz.landmark
 * @apiviz.uses org.jboss.netty.util.HashedWheelTimer
 * @apiviz.has org.jboss.netty.handler.timeout.IdleStateEvent oneway - - triggers
 */
@ChannelPipelineCoverage("one")
public class IdleStateHandler extends SimpleChannelUpstreamHandler
                             implements LifeCycleAwareChannelHandler,
                                        ExternalResourceReleasable {

    final Timer timer;

    final long readerIdleTimeMillis;
    volatile Timeout readerIdleTimeout;
    volatile long lastReadTime;

    final long writerIdleTimeMillis;
    volatile Timeout writerIdleTimeout;
    volatile long lastWriteTime;

    final long allIdleTimeMillis;
    volatile Timeout allIdleTimeout;

    /**
     * Creates a new instance.
     *
     * @param timer
     *        the {@link Timer} that is used to trigger the scheduled event.
     *        The recommended {@link Timer} implementation is {@link HashedWheelTimer}.
     * @param readerIdleTimeSeconds
     *        an {@link IdleStateEvent} whose state is {@link IdleState#READER_IDLE}
     *        will be triggered when no read was performed for the specified
     *        period of time.  Specify {@code 0} to disable.
     * @param writerIdleTimeSeconds
     *        an {@link IdleStateEvent} whose state is {@link IdleState#WRITER_IDLE}
     *        will be triggered when no write was performed for the specified
     *        period of time.  Specify {@code 0} to disable.
     * @param allIdleTimeSeconds
     *        an {@link IdleStateEvent} whose state is {@link IdleState#ALL_IDLE}
     *        will be triggered when neither read nor write was performed for
     *        the specified period of time.  Specify {@code 0} to disable.
     */
    public IdleStateHandler(
            Timer timer,
            int readerIdleTimeSeconds,
            int writerIdleTimeSeconds,
            int allIdleTimeSeconds) {

        this(timer,
             readerIdleTimeSeconds, writerIdleTimeSeconds, allIdleTimeSeconds,
             TimeUnit.SECONDS);
    }

    /**
     * Creates a new instance.
     *
     * @param timer
     *        the {@link Timer} that is used to trigger the scheduled event.
     *        The recommended {@link Timer} implementation is {@link HashedWheelTimer}.
     * @param readerIdleTime
     *        an {@link IdleStateEvent} whose state is {@link IdleState#READER_IDLE}
     *        will be triggered when no read was performed for the specified
     *        period of time.  Specify {@code 0} to disable.
     * @param writerIdleTime
     *        an {@link IdleStateEvent} whose state is {@link IdleState#WRITER_IDLE}
     *        will be triggered when no write was performed for the specified
     *        period of time.  Specify {@code 0} to disable.
     * @param allIdleTime
     *        an {@link IdleStateEvent} whose state is {@link IdleState#ALL_IDLE}
     *        will be triggered when neither read nor write was performed for
     *        the specified period of time.  Specify {@code 0} to disable.
     * @param unit
     *        the {@link TimeUnit} of {@code readerIdleTime},
     *        {@code writeIdleTime}, and {@code allIdleTime}
     */
    public IdleStateHandler(
            Timer timer,
            long readerIdleTime, long writerIdleTime, long allIdleTime,
            TimeUnit unit) {

        if (timer == null) {
            throw new NullPointerException("timer");
        }
        if (unit == null) {
            throw new NullPointerException("unit");
        }

        this.timer = timer;
        if (readerIdleTime <= 0) {
            readerIdleTimeMillis = 0;
        } else {
            readerIdleTimeMillis = Math.max(unit.toMillis(readerIdleTime), 1);
        }
        if (writerIdleTime <= 0) {
            writerIdleTimeMillis = 0;
        } else {
            writerIdleTimeMillis = Math.max(unit.toMillis(writerIdleTime), 1);
        }
        if (allIdleTime <= 0) {
            allIdleTimeMillis = 0;
        } else {
            allIdleTimeMillis = Math.max(unit.toMillis(allIdleTime), 1);
        }
    }

    /**
     * Stops the {@link Timer} which was specified in the constructor of this
     * handler.  You should not call this method if the {@link Timer} is in use
     * by other objects.
     */
    public void releaseExternalResources() {
        timer.stop();
    }

    public void beforeAdd(ChannelHandlerContext ctx) throws Exception {
        initialize(ctx);
    }

    public void afterAdd(ChannelHandlerContext ctx) throws Exception {
        // NOOP
    }

    public void beforeRemove(ChannelHandlerContext ctx) throws Exception {
        destroy();
    }

    public void afterRemove(ChannelHandlerContext ctx) throws Exception {
        // NOOP
    }

    @Override
    public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e)
            throws Exception {
        initialize(ctx);
        ctx.sendUpstream(e);
    }

    @Override
    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e)
            throws Exception {
        destroy();
        ctx.sendUpstream(e);
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
            throws Exception {
        lastReadTime = System.currentTimeMillis();
        ctx.sendUpstream(e);
    }

    @Override
    public void writeComplete(ChannelHandlerContext ctx, WriteCompletionEvent e)
            throws Exception {
        if (e.getWrittenAmount() > 0) {
            lastWriteTime = System.currentTimeMillis();
        }
        ctx.sendUpstream(e);
    }

    private void initialize(ChannelHandlerContext ctx) {
        lastReadTime = lastWriteTime = System.currentTimeMillis();
        if (readerIdleTimeMillis > 0) {
            readerIdleTimeout = timer.newTimeout(
                    new ReaderIdleTimeoutTask(ctx),
                    readerIdleTimeMillis, TimeUnit.MILLISECONDS);
        }
        if (writerIdleTimeMillis > 0) {
            writerIdleTimeout = timer.newTimeout(
                    new WriterIdleTimeoutTask(ctx),
                    writerIdleTimeMillis, TimeUnit.MILLISECONDS);
        }
        if (allIdleTimeMillis > 0) {
            allIdleTimeout = timer.newTimeout(
                    new AllIdleTimeoutTask(ctx),
                    allIdleTimeMillis, TimeUnit.MILLISECONDS);
        }
    }

    private void destroy() {
        if (readerIdleTimeout != null) {
            readerIdleTimeout.cancel();
            readerIdleTimeout = null;
        }
        if (writerIdleTimeout != null) {
            writerIdleTimeout.cancel();
            writerIdleTimeout = null;
        }
        if (allIdleTimeout != null) {
            allIdleTimeout.cancel();
            allIdleTimeout = null;
        }
    }

    protected void channelIdle(
            ChannelHandlerContext ctx, IdleState state, long lastActivityTimeMillis) throws Exception {
        ctx.sendUpstream(new DefaultIdleStateEvent(ctx.getChannel(), state, lastActivityTimeMillis));
    }

    private final class ReaderIdleTimeoutTask implements TimerTask {

        private final ChannelHandlerContext ctx;

        ReaderIdleTimeoutTask(ChannelHandlerContext ctx) {
            this.ctx = ctx;
        }

        public void run(Timeout timeout) throws Exception {
            if (timeout.isCancelled() || !ctx.getChannel().isOpen()) {
                return;
            }

            long currentTime = System.currentTimeMillis();
            long lastReadTime = IdleStateHandler.this.lastReadTime;
            long nextDelay = readerIdleTimeMillis - (currentTime - lastReadTime);
            if (nextDelay <= 0) {
                // Reader is idle - set a new timeout and notify the callback.
                readerIdleTimeout =
                    timer.newTimeout(this, readerIdleTimeMillis, TimeUnit.MILLISECONDS);
                try {
                    channelIdle(ctx, IdleState.READER_IDLE, lastReadTime);
                } catch (Throwable t) {
                    fireExceptionCaught(ctx, t);
                }
            } else {
                // Read occurred before the timeout - set a new timeout with shorter delay.
                readerIdleTimeout =
                    timer.newTimeout(this, nextDelay, TimeUnit.MILLISECONDS);
            }
        }

    }

    private final class WriterIdleTimeoutTask implements TimerTask {

        private final ChannelHandlerContext ctx;

        WriterIdleTimeoutTask(ChannelHandlerContext ctx) {
            this.ctx = ctx;
        }

        public void run(Timeout timeout) throws Exception {
            if (timeout.isCancelled() || !ctx.getChannel().isOpen()) {
                return;
            }

            long currentTime = System.currentTimeMillis();
            long lastWriteTime = IdleStateHandler.this.lastWriteTime;
            long nextDelay = writerIdleTimeMillis - (currentTime - lastWriteTime);
            if (nextDelay <= 0) {
                // Writer is idle - set a new timeout and notify the callback.
                writerIdleTimeout =
                    timer.newTimeout(this, writerIdleTimeMillis, TimeUnit.MILLISECONDS);
                try {
                    channelIdle(ctx, IdleState.WRITER_IDLE, lastWriteTime);
                } catch (Throwable t) {
                    fireExceptionCaught(ctx, t);
                }
            } else {
                // Write occurred before the timeout - set a new timeout with shorter delay.
                writerIdleTimeout =
                    timer.newTimeout(this, nextDelay, TimeUnit.MILLISECONDS);
            }
        }
    }

    private final class AllIdleTimeoutTask implements TimerTask {

        private final ChannelHandlerContext ctx;

        AllIdleTimeoutTask(ChannelHandlerContext ctx) {
            this.ctx = ctx;
        }

        public void run(Timeout timeout) throws Exception {
            if (timeout.isCancelled() || !ctx.getChannel().isOpen()) {
                return;
            }

            long currentTime = System.currentTimeMillis();
            long lastIoTime = Math.max(lastReadTime, lastWriteTime);
            long nextDelay = allIdleTimeMillis - (currentTime - lastIoTime);
            if (nextDelay <= 0) {
                // Both reader and writer are idle - set a new timeout and
                // notify the callback.
                allIdleTimeout =
                    timer.newTimeout(this, allIdleTimeMillis, TimeUnit.MILLISECONDS);
                try {
                    channelIdle(ctx, IdleState.ALL_IDLE, lastIoTime);
                } catch (Throwable t) {
                    fireExceptionCaught(ctx, t);
                }
            } else {
                // Either read or write occurred before the timeout - set a new
                // timeout with shorter delay.
                allIdleTimeout =
                    timer.newTimeout(this, nextDelay, TimeUnit.MILLISECONDS);
            }
        }
    }
}
