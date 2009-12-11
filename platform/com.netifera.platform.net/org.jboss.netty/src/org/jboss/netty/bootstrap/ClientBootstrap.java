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
package org.jboss.netty.bootstrap;

import static org.jboss.netty.channel.Channels.*;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.NotYetConnectedException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelConfig;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ChannelPipelineException;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

/**
 * A helper class which creates a new client-side {@link Channel} and makes a
 * connection attempt.
 *
 * <h3>Configuring a channel</h3>
 *
 * {@link #setOption(String, Object) Options} are used to configure a channel:
 *
 * <pre>
 * ClientBootstrap b = ...;
 *
 * // Options for a new channel
 * b.setOption("remoteAddress", new {@link InetSocketAddress}("example.com", 8080));
 * b.setOption("tcpNoDelay", true);
 * b.setOption("receiveBufferSize", 1048576);
 * </pre>
 *
 * For the detailed list of available options, please refer to
 * {@link ChannelConfig} and its sub-types
 *
 * <h3>Configuring a channel pipeline</h3>
 *
 * Every channel has its own {@link ChannelPipeline} and you can configure it
 * in two ways.
 * <p>
 * {@linkplain #setPipeline(ChannelPipeline) The first approach} is to use
 * the default pipeline and let the bootstrap to shallow-copy the default
 * pipeline for each new channel:
 *
 * <pre>
 * ClientBootstrap b = ...;
 * {@link ChannelPipeline} p = b.getPipeline();
 *
 * // Add handlers to the pipeline.
 * p.addLast("encoder", new EncodingHandler());
 * p.addLast("decoder", new DecodingHandler());
 * p.addLast("logic",   new LogicHandler());
 * </pre>
 *
 * Please note 'shallow-copy' here means that the added {@link ChannelHandler}s
 * are not cloned but only their references are added to the new pipeline.
 * Therefore, you have to choose the second approach if you are going to open
 * more than one {@link Channel} whose {@link ChannelPipeline} contains any
 * {@link ChannelHandler} whose {@link ChannelPipelineCoverage} is {@code "one"}.
 *
 * <p>
 * {@linkplain #setPipelineFactory(ChannelPipelineFactory) The second approach}
 * is to specify a {@link ChannelPipelineFactory} by yourself and have full
 * control over how a new pipeline is created.  This approach is more complex
 * than the first approach while it is much more flexible:
 *
 * <pre>
 * ClientBootstrap b = ...;
 * b.setPipelineFactory(new MyPipelineFactory());
 *
 * public class MyPipelineFactory implements {@link ChannelPipelineFactory} {
 *   // Create a new pipeline for a new channel and configure it here ...
 * }
 * </pre>
 *
 * <h3>Applying different settings for different {@link Channel}s</h3>
 *
 * {@link ClientBootstrap} is just a helper class.  It neither allocates nor
 * manages any resources.  What manages the resources is the
 * {@link ChannelFactory} implementation you specified in the constructor of
 * {@link ClientBootstrap}.  Therefore, it is OK to create as many
 * {@link ClientBootstrap} instances as you want to apply different settings
 * for different {@link Channel}s.
 *
 * @author The Netty Project (netty-dev@lists.jboss.org)
 * @author Trustin Lee (trustin@gmail.com)
 *
 * @version $Rev: 1783 $, $Date: 2009-10-14 14:46:40 +0900 (수, 14 10 2009) $
 *
 * @apiviz.landmark
 */
public class ClientBootstrap extends Bootstrap {

    /**
     * Creates a new instance with no {@link ChannelFactory} set.
     * {@link #setFactory(ChannelFactory)} must be called before any I/O
     * operation is requested.
     */
    public ClientBootstrap() {
        super();
    }

    /**
     * Creates a new instance with the specified initial {@link ChannelFactory}.
     */
    public ClientBootstrap(ChannelFactory channelFactory) {
        super(channelFactory);
    }

    /**
     * Attempts a new connection with the current {@code "remoteAddress"} and
     * {@code "localAddress"} option.  If the {@code "localAddress"} option is
     * not set, the local address of a new channel is determined automatically.
     * This method is similar to the following code:
     *
     * <pre>
     * ClientBootstrap b = ...;
     * b.connect(b.getOption("remoteAddress"), b.getOption("localAddress"));
     * </pre>
     *
     * @return a future object which notifies when this connection attempt
     *         succeeds or fails
     *
     * @throws IllegalStateException
     *         if {@code "remoteAddress"} option was not set
     * @throws ClassCastException
     *         if {@code "remoteAddress"} or {@code "localAddress"} option's
     *            value is neither a {@link SocketAddress} nor {@code null}
     * @throws ChannelPipelineException
     *         if this bootstrap's {@link #setPipelineFactory(ChannelPipelineFactory) pipelineFactory}
     *            failed to create a new {@link ChannelPipeline}
     */
    public ChannelFuture connect() {
        SocketAddress remoteAddress = (SocketAddress) getOption("remoteAddress");
        if (remoteAddress == null) {
            throw new IllegalStateException("remoteAddress option is not set.");
        }
        return connect(remoteAddress);
    }

    /**
     * Attempts a new connection with the specified {@code remoteAddress} and
     * the current {@code "localAddress"} option. If the {@code "localAddress"}
     * option is not set, the local address of a new channel is determined
     * automatically.  This method is identical with the following code:
     *
     * <pre>
     * ClientBootstrap b = ...;
     * b.connect(remoteAddress, b.getOption("localAddress"));
     * </pre>
     *
     * @return a future object which notifies when this connection attempt
     *         succeeds or fails
     *
     * @throws ClassCastException
     *         if {@code "localAddress"} option's value is
     *            neither a {@link SocketAddress} nor {@code null}
     * @throws ChannelPipelineException
     *         if this bootstrap's {@link #setPipelineFactory(ChannelPipelineFactory) pipelineFactory}
     *            failed to create a new {@link ChannelPipeline}
     */
    public ChannelFuture connect(SocketAddress remoteAddress) {
        if (remoteAddress == null) {
            throw new NullPointerException("remotedAddress");
        }
        SocketAddress localAddress = (SocketAddress) getOption("localAddress");
        return connect(remoteAddress, localAddress);
    }

    /**
     * Attempts a new connection with the specified {@code remoteAddress} and
     * the specified {@code localAddress}.  If the specified local address is
     * {@code null}, the local address of a new channel is determined
     * automatically.
     *
     * @return a future object which notifies when this connection attempt
     *         succeeds or fails
     *
     * @throws ChannelPipelineException
     *         if this bootstrap's {@link #setPipelineFactory(ChannelPipelineFactory) pipelineFactory}
     *            failed to create a new {@link ChannelPipeline}
     */
    public ChannelFuture connect(final SocketAddress remoteAddress, final SocketAddress localAddress) {

        if (remoteAddress == null) {
            throw new NullPointerException("remoteAddress");
        }

        final BlockingQueue<ChannelFuture> futureQueue =
            new LinkedBlockingQueue<ChannelFuture>();

        ChannelPipeline pipeline;
        try {
            pipeline = getPipelineFactory().getPipeline();
        } catch (Exception e) {
            throw new ChannelPipelineException("Failed to initialize a pipeline.", e);
        }

        pipeline.addFirst(
                "connector", new Connector(
                        this, remoteAddress, localAddress, futureQueue));

        getFactory().newChannel(pipeline);

        // Wait until the future is available.
        ChannelFuture future = null;
        boolean interrupted = false;
        do {
            try {
                future = futureQueue.poll(Integer.MAX_VALUE, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                interrupted = true;
            }
        } while (future == null);

        pipeline.remove("connector");

        if (interrupted) {
            Thread.currentThread().interrupt();
        }

        return future;
    }

    @ChannelPipelineCoverage("one")
    static final class Connector extends SimpleChannelUpstreamHandler {
        private final Bootstrap bootstrap;
        private final SocketAddress localAddress;
        private final BlockingQueue<ChannelFuture> futureQueue;
        private final SocketAddress remoteAddress;
        private volatile boolean finished = false;

        Connector(
                Bootstrap bootstrap,
                SocketAddress remoteAddress,
                SocketAddress localAddress,
                BlockingQueue<ChannelFuture> futureQueue) {
            this.bootstrap = bootstrap;
            this.localAddress = localAddress;
            this.futureQueue = futureQueue;
            this.remoteAddress = remoteAddress;
        }

        @Override
        public void channelOpen(
                ChannelHandlerContext context,
                ChannelStateEvent event) {

            try {
                // Apply options.
                event.getChannel().getConfig().setOptions(bootstrap.getOptions());
            } finally {
                context.sendUpstream(event);
            }

            // Bind or connect.
            if (localAddress != null) {
                event.getChannel().bind(localAddress);
            } else {
                finished = futureQueue.offer(event.getChannel().connect(remoteAddress));
                assert finished;
            }
        }

        @Override
        public void channelBound(
                ChannelHandlerContext context,
                ChannelStateEvent event) {
            context.sendUpstream(event);

            // Connect if not connected yet.
            if (localAddress != null) {
                finished = futureQueue.offer(event.getChannel().connect(remoteAddress));
                assert finished;
            }
        }

        @Override
        public void exceptionCaught(
                ChannelHandlerContext ctx, ExceptionEvent e)
                throws Exception {
            ctx.sendUpstream(e);

            Throwable cause = e.getCause();
            if (!(cause instanceof NotYetConnectedException) && !finished) {
                e.getChannel().close();
                finished = futureQueue.offer(failedFuture(e.getChannel(), cause));
                assert finished;
            }
        }
    }
}
