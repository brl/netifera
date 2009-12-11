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
package org.jboss.netty.channel.socket.nio;

import static org.jboss.netty.channel.Channels.*;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.netty.channel.AbstractChannelSink;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelState;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.MessageEvent;

/**
 * Receives downstream events from a {@link ChannelPipeline}.  It contains
 * an array of I/O workers.
 *
 * @author The Netty Project (netty-dev@lists.jboss.org)
 * @author Trustin Lee (trustin@gmail.com)
 * @author Daniel Bevenius (dbevenius@jboss.com)
 *
 * @version $Rev: 1783 $, $Date: 2009-10-14 14:46:40 +0900 (수, 14 10 2009) $
 */
class NioDatagramPipelineSink extends AbstractChannelSink {

    private static final AtomicInteger nextId = new AtomicInteger();

    private final int id = nextId.incrementAndGet();

    private final NioDatagramWorker[] workers;

    private final AtomicInteger workerIndex = new AtomicInteger();

    /**
     * Creates a new {@link NioDatagramPipelineSink} with a the number of {@link NioDatagramWorker}s specified in workerCount.
     * The {@link NioDatagramWorker}s take care of reading and writing for the {@link NioDatagramChannel}.
     *
     * @param workerExecutor
     * @param workerCount The number of UdpWorkers for this sink.
     */
    NioDatagramPipelineSink(final Executor workerExecutor, final int workerCount) {
        workers = new NioDatagramWorker[workerCount];
        for (int i = 0; i < workers.length; i ++) {
            workers[i] = new NioDatagramWorker(id, i + 1, workerExecutor);
        }
    }

    /**
     * Handle downstream event.
     *
     * @param pipeline the {@link ChannelPipeline} that passes down the
     *                 downstream event.
     * @param e The downstream event.
     */
    public void eventSunk(final ChannelPipeline pipeline, final ChannelEvent e)
            throws Exception {
        final NioDatagramChannel channel = (NioDatagramChannel) e.getChannel();
        final ChannelFuture future = e.getFuture();
        if (e instanceof ChannelStateEvent) {
            final ChannelStateEvent stateEvent = (ChannelStateEvent) e;
            final ChannelState state = stateEvent.getState();
            final Object value = stateEvent.getValue();
            switch (state) {
            case OPEN:
                if (Boolean.FALSE.equals(value)) {
                    NioDatagramWorker.close(channel, future);
                }
                break;
            case BOUND:
                if (value != null) {
                    bind(channel, future, (InetSocketAddress) value);
                } else {
                    NioDatagramWorker.close(channel, future);
                }
                break;
            case CONNECTED:
                if (value != null) {
                    connect(channel, future, (InetSocketAddress) value);
                } else {
                    NioDatagramWorker.disconnect(channel, future);
                }
                break;
            case INTEREST_OPS:
                NioDatagramWorker.setInterestOps(channel, future, ((Integer) value)
                        .intValue());
                break;
            }
        } else if (e instanceof MessageEvent) {
            final MessageEvent event = (MessageEvent) e;
            final boolean offered = channel.writeBufferQueue.offer(event);
            assert offered;
            NioDatagramWorker.write(channel, true);
        }
    }

    private void close(NioDatagramChannel channel, ChannelFuture future) {
        try {
            channel.getDatagramChannel().socket().close();
            future.setSuccess();
            if (channel.setClosed()) {
                if (channel.isBound()) {
                    fireChannelUnbound(channel);
                }
                fireChannelClosed(channel);
            }
        } catch (final Throwable t) {
            future.setFailure(t);
            fireExceptionCaught(channel, t);
        }
    }

    /**
     * Will bind the DatagramSocket to the passed-in address.
     * Every call bind will spawn a new thread using the that basically in turn
     */
    private void bind(final NioDatagramChannel channel,
            final ChannelFuture future, final InetSocketAddress address) {
        boolean bound = false;
        boolean started = false;
        try {
            // First bind the DatagramSocket the specified port.
            channel.getDatagramChannel().socket().bind(address);
            bound = true;

            future.setSuccess();
            fireChannelBound(channel, address);

            channel.worker.register(channel, null);
            started = true;
        } catch (final Throwable t) {
            future.setFailure(t);
            fireExceptionCaught(channel, t);
        } finally {
            if (!started && bound) {
                close(channel, future);
            }
        }
    }

    private void connect(
            NioDatagramChannel channel, ChannelFuture future,
            SocketAddress remoteAddress) {

        boolean bound = channel.isBound();
        boolean connected = false;
        boolean workerStarted = false;

        future.addListener(ChannelFutureListener.CLOSE_ON_FAILURE);

        // Clear the cached address so that the next getRemoteAddress() call
        // updates the cache.
        channel.remoteAddress = null;

        try {
            channel.getDatagramChannel().connect(remoteAddress);
            connected = true;

            // Fire events.
            future.setSuccess();
            if (!bound) {
                fireChannelBound(channel, channel.getLocalAddress());
            }
            fireChannelConnected(channel, channel.getRemoteAddress());

            if (!bound) {
                channel.worker.register(channel, future);
            }

            workerStarted = true;
        } catch (Throwable t) {
            future.setFailure(t);
            fireExceptionCaught(channel, t);
        } finally {
            if (connected && !workerStarted) {
                NioDatagramWorker.close(channel, future);
            }
        }
    }

    NioDatagramWorker nextWorker() {
        return workers[Math.abs(workerIndex.getAndIncrement() % workers.length)];
    }

}
