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
package org.jboss.netty.handler.ssl;

import static org.jboss.netty.channel.Channels.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLEngineResult.Status;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelDownstreamHandler;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.DownstreamMessageEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.frame.FrameDecoder;
import org.jboss.netty.logging.InternalLogger;
import org.jboss.netty.logging.InternalLoggerFactory;
import org.jboss.netty.util.internal.ImmediateExecutor;

/**
 * Adds <a href="http://en.wikipedia.org/wiki/Transport_Layer_Security">SSL
 * &middot; TLS</a> and StartTLS support to a {@link Channel}.  Please refer
 * to the <strong>"SecureChat"</strong> example in the distribution or the web
 * site for the detailed usage.
 *
 * <h3>Beginning the handshake</h3>
 * <p>
 * You must make sure not to write a message while the
 * {@linkplain #handshake(Channel) handshake} is in progress unless you are
 * renegotiating.  You will be notified by the {@link ChannelFuture} which is
 * returned by the {@link #handshake(Channel)} method when the handshake
 * process succeeds or fails.
 *
 * <h3>Renegotiation</h3>
 * <p>
 * Once the initial handshake is done successfully.  You can always call
 * {@link #handshake(Channel)} again to renegotiate the SSL session parameters.
 *
 * <h3>Closing the session</h3>
 * <p>
 * To close the SSL session, the {@link #close(Channel)} method should be
 * called to send the {@code close_notify} message to the remote peer.  One
 * exception is when you close the {@link Channel} - {@link SslHandler}
 * intercepts the close request and send the {@code close_notify} message
 * before the channel closure automatically.  Once the SSL session is closed,
 * it is not reusable, and consequently you should create a new
 * {@link SslHandler} with a new {@link SSLEngine} as explained in the
 * following section.
 *
 * <h3>Restarting the session</h3>
 * <p>
 * To restart the SSL session, you must remove the existing closed
 * {@link SslHandler} from the {@link ChannelPipeline}, insert a new
 * {@link SslHandler} with a new {@link SSLEngine} into the pipeline,
 * and start the handshake process as described in the first section.
 *
 * <h3>Implementing StartTLS</h3>
 * <p>
 * <a href="http://en.wikipedia.org/wiki/STARTTLS">StartTLS</a> is the
 * communication pattern that secures the wire in the middle of the plaintext
 * connection.  Please note that it is different from SSL &middot; TLS, that
 * secures the wire from the beginning of the connection.  Typically, StartTLS
 * is composed of three steps:
 * <ol>
 * <li>Client sends a StartTLS request to server.</li>
 * <li>Server sends a StartTLS response to client.</li>
 * <li>Client begins SSL handshake.</li>
 * </ol>
 * If you implement a server, you need to:
 * <ol>
 * <li>create a new {@link SslHandler} instance with {@code startTls} flag set
 *     to {@code true},</li>
 * <li>insert the {@link SslHandler} to the {@link ChannelPipeline}, and</li>
 * <li>write a StartTLS response.</li>
 * </ol>
 * Please note that you must insert {@link SslHandler} <em>before</em> sending
 * the StartTLS response.  Otherwise the client can send begin SSL handshake
 * before {@link SslHandler} is inserted to the {@link ChannelPipeline}, causing
 * data corruption.
 * <p>
 * The client-side implementation is much simpler.
 * <ol>
 * <li>Write a StartTLS request,</li>
 * <li>wait for the StartTLS response,</li>
 * <li>create a new {@link SslHandler} instance with {@code startTls} flag set
 *     to {@code false},</li>
 * <li>insert the {@link SslHandler} to the {@link ChannelPipeline}, and</li>
 * <li>Initiate SSL handshake by calling {@link SslHandler#handshake(Channel)}.</li>
 * </ol>
 *
 * @author The Netty Project (netty-dev@lists.jboss.org)
 * @author Trustin Lee (trustin@gmail.com)
 *
 * @version $Rev: 1827 $, $Date: 2009-10-22 19:29:39 +0900 (목, 22 10 2009) $
 *
 * @apiviz.landmark
 * @apiviz.uses org.jboss.netty.handler.ssl.SslBufferPool
 */
public class SslHandler extends FrameDecoder implements ChannelDownstreamHandler {

    private static final InternalLogger logger =
        InternalLoggerFactory.getInstance(SslHandler.class);

    private static final ByteBuffer EMPTY_BUFFER = ByteBuffer.allocate(0);

    private static final Pattern IGNORABLE_ERROR_MESSAGE = Pattern.compile(
            "^.*(?:connection.*reset|connection.*closed|broken.*pipe).*$",
            Pattern.CASE_INSENSITIVE);

    private static SslBufferPool defaultBufferPool;

    /**
     * Returns the default {@link SslBufferPool} used when no pool is
     * specified in the constructor.
     */
    public static synchronized SslBufferPool getDefaultBufferPool() {
        if (defaultBufferPool == null) {
            defaultBufferPool = new SslBufferPool();
        }
        return defaultBufferPool;
    }

    private final SSLEngine engine;
    private final SslBufferPool bufferPool;
    private final Executor delegatedTaskExecutor;
    private final boolean startTls;

    final Object handshakeLock = new Object();
    private boolean initialHandshake;
    private boolean handshaking;
    private volatile boolean handshaken;
    private volatile ChannelFuture handshakeFuture;

    private final AtomicBoolean sentFirstMessage = new AtomicBoolean();
    private final AtomicBoolean sentCloseNotify = new AtomicBoolean();
    int ignoreClosedChannelException;
    final Object ignoreClosedChannelExceptionLock = new Object();
    private final Queue<PendingWrite> pendingUnencryptedWrites = new LinkedList<PendingWrite>();
    private final Queue<MessageEvent> pendingEncryptedWrites = new LinkedList<MessageEvent>();

    /**
     * Creates a new instance.
     *
     * @param engine  the {@link SSLEngine} this handler will use
     */
    public SslHandler(SSLEngine engine) {
        this(engine, getDefaultBufferPool(), ImmediateExecutor.INSTANCE);
    }

    /**
     * Creates a new instance.
     *
     * @param engine      the {@link SSLEngine} this handler will use
     * @param bufferPool  the {@link SslBufferPool} where this handler will
     *                    acquire the buffers required by the {@link SSLEngine}
     */
    public SslHandler(SSLEngine engine, SslBufferPool bufferPool) {
        this(engine, bufferPool, ImmediateExecutor.INSTANCE);
    }

    /**
     * Creates a new instance.
     *
     * @param engine    the {@link SSLEngine} this handler will use
     * @param startTls  {@code true} if the first write request shouldn't be
     *                  encrypted by the {@link SSLEngine}
     */
    public SslHandler(SSLEngine engine, boolean startTls) {
        this(engine, getDefaultBufferPool(), startTls);
    }

    /**
     * Creates a new instance.
     *
     * @param engine      the {@link SSLEngine} this handler will use
     * @param bufferPool  the {@link SslBufferPool} where this handler will
     *                    acquire the buffers required by the {@link SSLEngine}
     * @param startTls    {@code true} if the first write request shouldn't be
     *                    encrypted by the {@link SSLEngine}
     */
    public SslHandler(SSLEngine engine, SslBufferPool bufferPool, boolean startTls) {
        this(engine, bufferPool, startTls, ImmediateExecutor.INSTANCE);
    }

    /**
     * Creates a new instance.
     *
     * @param engine
     *        the {@link SSLEngine} this handler will use
     * @param delegatedTaskExecutor
     *        the {@link Executor} which will execute the delegated task
     *        that {@link SSLEngine#getDelegatedTask()} will return
     */
    public SslHandler(SSLEngine engine, Executor delegatedTaskExecutor) {
        this(engine, getDefaultBufferPool(), delegatedTaskExecutor);
    }

    /**
     * Creates a new instance.
     *
     * @param engine
     *        the {@link SSLEngine} this handler will use
     * @param bufferPool
     *        the {@link SslBufferPool} where this handler will acquire
     *        the buffers required by the {@link SSLEngine}
     * @param delegatedTaskExecutor
     *        the {@link Executor} which will execute the delegated task
     *        that {@link SSLEngine#getDelegatedTask()} will return
     */
    public SslHandler(SSLEngine engine, SslBufferPool bufferPool, Executor delegatedTaskExecutor) {
        this(engine, bufferPool, false, delegatedTaskExecutor);
    }

    /**
     * Creates a new instance.
     *
     * @param engine
     *        the {@link SSLEngine} this handler will use
     * @param startTls
     *        {@code true} if the first write request shouldn't be encrypted
     *        by the {@link SSLEngine}
     * @param delegatedTaskExecutor
     *        the {@link Executor} which will execute the delegated task
     *        that {@link SSLEngine#getDelegatedTask()} will return
     */
    public SslHandler(SSLEngine engine, boolean startTls, Executor delegatedTaskExecutor) {
        this(engine, getDefaultBufferPool(), startTls, delegatedTaskExecutor);
    }

    /**
     * Creates a new instance.
     *
     * @param engine
     *        the {@link SSLEngine} this handler will use
     * @param bufferPool
     *        the {@link SslBufferPool} where this handler will acquire
     *        the buffers required by the {@link SSLEngine}
     * @param startTls
     *        {@code true} if the first write request shouldn't be encrypted
     *        by the {@link SSLEngine}
     * @param delegatedTaskExecutor
     *        the {@link Executor} which will execute the delegated task
     *        that {@link SSLEngine#getDelegatedTask()} will return
     */
    public SslHandler(SSLEngine engine, SslBufferPool bufferPool, boolean startTls, Executor delegatedTaskExecutor) {
        if (engine == null) {
            throw new NullPointerException("engine");
        }
        if (bufferPool == null) {
            throw new NullPointerException("bufferPool");
        }
        if (delegatedTaskExecutor == null) {
            throw new NullPointerException("delegatedTaskExecutor");
        }
        this.engine = engine;
        this.bufferPool = bufferPool;
        this.delegatedTaskExecutor = delegatedTaskExecutor;
        this.startTls = startTls;
    }

    /**
     * Returns the {@link SSLEngine} which is used by this handler.
     */
    public SSLEngine getEngine() {
        return engine;
    }

    /**
     * Starts an SSL / TLS handshake for the specified channel.
     *
     * @return a {@link ChannelFuture} which is notified when the handshake
     *         succeeds or fails.
     */
    public ChannelFuture handshake(Channel channel) {
        ChannelFuture handshakeFuture;
        synchronized (handshakeLock) {
            if (handshaking) {
                return this.handshakeFuture;
            } else {
                handshaking = true;
                try {
                    engine.beginHandshake();
                    runDelegatedTasks();
                    handshakeFuture = this.handshakeFuture = future(channel);
                } catch (SSLException e) {
                    handshakeFuture = this.handshakeFuture = failedFuture(channel, e);
                }
            }
        }

        try {
            wrapNonAppData(context(channel), channel);
        } catch (SSLException e) {
            handshakeFuture.setFailure(e);
        }
        return handshakeFuture;
    }

    /**
     * Sends an SSL {@code close_notify} message to the specified channel and
     * destroys the underlying {@link SSLEngine}.
     */
    public ChannelFuture close(Channel channel) {
        try {
            ChannelHandlerContext ctx = context(channel);
            engine.closeOutbound();
            return wrapNonAppData(ctx, channel);
        } catch (SSLException e) {
            return failedFuture(channel, e);
        }
    }

    private ChannelHandlerContext context(Channel channel) {
        return channel.getPipeline().getContext(this);
    }

    public void handleDownstream(
            final ChannelHandlerContext context, final ChannelEvent evt) throws Exception {
        if (evt instanceof ChannelStateEvent) {
            ChannelStateEvent e = (ChannelStateEvent) evt;
            switch (e.getState()) {
            case OPEN:
            case CONNECTED:
            case BOUND:
                if (Boolean.FALSE.equals(e.getValue()) || e.getValue() == null) {
                    closeOutboundAndChannel(context, e);
                    return;
                }
            }
        }
        if (!(evt instanceof MessageEvent)) {
            context.sendDownstream(evt);
            return;
        }

        MessageEvent e = (MessageEvent) evt;
        if (!(e.getMessage() instanceof ChannelBuffer)) {
            context.sendDownstream(evt);
            return;
        }

        // Do not encrypt the first write request if this handler is
        // created with startTLS flag turned on.
        if (startTls && sentFirstMessage.compareAndSet(false, true)) {
            context.sendDownstream(evt);
            return;
        }

        // Otherwise, all messages are encrypted.
        ChannelBuffer msg = (ChannelBuffer) e.getMessage();
        PendingWrite pendingWrite;

        if (msg.readable()) {
            pendingWrite = new PendingWrite(evt.getFuture(), msg.toByteBuffer(msg.readerIndex(), msg.readableBytes()));
        } else {
            pendingWrite = new PendingWrite(evt.getFuture(), null);
        }
        synchronized (pendingUnencryptedWrites) {
            boolean offered = pendingUnencryptedWrites.offer(pendingWrite);
            assert offered;
        }

        wrap(context, evt.getChannel());
    }

    @Override
    public void channelDisconnected(ChannelHandlerContext ctx,
            ChannelStateEvent e) throws Exception {

        // Make sure the handshake future is notified when a connection has
        // been closed during handshake.
        synchronized (handshakeLock) {
            if (handshaking) {
                handshakeFuture.setFailure(new ClosedChannelException());
            }
        }

        try {
            super.channelDisconnected(ctx, e);
        } finally {
            unwrap(ctx, e.getChannel(), ChannelBuffers.EMPTY_BUFFER, 0, 0);
            engine.closeOutbound();
            if (!sentCloseNotify.get() && handshaken) {
                try {
                    engine.closeInbound();
                } catch (SSLException ex) {
                    logger.debug("Failed to clean up SSLEngine.", ex);
                }
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
            throws Exception {

        Throwable cause = e.getCause();
        if (cause instanceof IOException) {
            if (cause instanceof ClosedChannelException) {
                synchronized (ignoreClosedChannelExceptionLock) {
                    if (ignoreClosedChannelException > 0) {
                        ignoreClosedChannelException --;
                        logger.debug(
                                "Swallowing an exception raised while " +
                                "writing non-app data", cause);
                        return;
                    }
                }
            } else if (engine.isOutboundDone()) {
                String message = String.valueOf(cause.getMessage()).toLowerCase();
                if (IGNORABLE_ERROR_MESSAGE.matcher(message).matches()) {
                    // It is safe to ignore the 'connection reset by peer' or
                    // 'broken pipe' error after sending closure_notify.
                    logger.debug(
                            "Swallowing a 'connection reset by peer / " +
                            "broken pipe' error occurred while writing " +
                            "'closure_notify'", cause);

                    // Close the connection explicitly just in case the transport
                    // did not close the connection automatically.
                    Channels.close(ctx, succeededFuture(e.getChannel()));
                    return;
                }
            }
        }

        ctx.sendUpstream(e);
    }

    @Override
    protected Object decode(
            final ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer) throws Exception {
        if (buffer.readableBytes() < 2) {
            return null;
        }

        int packetLength = buffer.getShort(buffer.readerIndex()) & 0xFFFF;
        if ((packetLength & 0x8000) != 0) {
            // Detected a SSLv2 packet
            packetLength &= 0x7FFF;
            packetLength += 2;
        } else  if (buffer.readableBytes() < 5) {
            return null;
        } else {
            // Detected a SSLv3 / TLSv1 packet
            packetLength = (buffer.getShort(buffer.readerIndex() + 3) & 0xFFFF) + 5;
        }

        if (buffer.readableBytes() < packetLength) {
            return null;
        }

        // We advance the buffer's readerIndex before calling unwrap() because
        // unwrap() can trigger FrameDecoder call decode(), this method, recursively.
        // The recursive call results in decoding the same packet twice if
        // the readerIndex is advanced *after* decode().
        //
        // Here's an example:
        // 1) An SSL packet is received from the wire.
        // 2) SslHandler.decode() deciphers the packet and calls the user code.
        // 3) The user closes the channel in the same thread.
        // 4) The same thread triggers a channelDisconnected() event.
        // 5) FrameDecoder.cleanup() is called, and it calls SslHandler.decode().
        // 6) SslHandler.decode() will feed the same packet with what was
        //    deciphered at the step 2 again if the readerIndex was not advanced
        //    before calling the user code.
        final int packetOffset = buffer.readerIndex();
        buffer.skipBytes(packetLength);
        return unwrap(ctx, channel, buffer, packetOffset, packetLength);
    }

    private ChannelFuture wrap(ChannelHandlerContext context, Channel channel)
            throws SSLException {

        ChannelFuture future = null;
        ChannelBuffer msg;
        ByteBuffer outNetBuf = bufferPool.acquire();
        boolean success = true;
        boolean offered = false;
        boolean needsUnwrap = false;
        try {
            loop:
            for (;;) {
                // Acquire a lock to make sure unencrypted data is polled
                // in order and their encrypted counterpart is offered in
                // order.
                synchronized (pendingUnencryptedWrites) {
                    PendingWrite pendingWrite = pendingUnencryptedWrites.peek();
                    if (pendingWrite == null) {
                        break;
                    }

                    ByteBuffer outAppBuf = pendingWrite.outAppBuf;
                    if (outAppBuf == null) {
                        // A write request with an empty buffer
                        pendingUnencryptedWrites.remove();
                        offerEncryptedWriteRequest(
                                new DownstreamMessageEvent(
                                        channel, pendingWrite.future,
                                        ChannelBuffers.EMPTY_BUFFER,
                                        channel.getRemoteAddress()));
                        offered = true;
                    } else {
                        SSLEngineResult result = null;
                        try {
                            synchronized (handshakeLock) {
                                result = engine.wrap(outAppBuf, outNetBuf);
                            }
                        } finally {
                            if (!outAppBuf.hasRemaining()) {
                                pendingUnencryptedWrites.remove();
                            }
                        }

                        if (result.bytesProduced() > 0) {
                            outNetBuf.flip();
                            msg = ChannelBuffers.buffer(outNetBuf.remaining());
                            msg.writeBytes(outNetBuf.array(), 0, msg.capacity());
                            outNetBuf.clear();

                            if (pendingWrite.outAppBuf.hasRemaining()) {
                                // pendingWrite's future shouldn't be notified if
                                // only partial data is written.
                                future = succeededFuture(channel);
                            } else {
                                future = pendingWrite.future;
                            }

                            MessageEvent encryptedWrite = new DownstreamMessageEvent(
                                    channel, future, msg, channel.getRemoteAddress());
                            offerEncryptedWriteRequest(encryptedWrite);
                            offered = true;
                        } else {
                            HandshakeStatus handshakeStatus = result.getHandshakeStatus();
                            switch (handshakeStatus) {
                            case NEED_WRAP:
                                if (outAppBuf.hasRemaining()) {
                                    break;
                                } else {
                                    break loop;
                                }
                            case NEED_UNWRAP:
                                needsUnwrap = true;
                                break loop;
                            case NEED_TASK:
                                runDelegatedTasks();
                                break;
                            case FINISHED:
                            case NOT_HANDSHAKING:
                                if (handshakeStatus == HandshakeStatus.FINISHED) {
                                    setHandshakeSuccess(channel);
                                }
                                if (result.getStatus() == Status.CLOSED) {
                                    success = false;
                                }
                                break loop;
                            default:
                                throw new IllegalStateException(
                                        "Unknown handshake status: " +
                                        result.getHandshakeStatus());
                            }
                        }
                    }
                }
            }
        } catch (SSLException e) {
            success = false;
            setHandshakeFailure(channel, e);
            throw e;
        } finally {
            bufferPool.release(outNetBuf);

            if (offered) {
                flushPendingEncryptedWrites(context);
            }

            if (!success) {
                IllegalStateException cause =
                    new IllegalStateException("SSLEngine already closed");
                // Mark all remaining pending writes as failure if anything
                // wrong happened before the write requests are wrapped.
                // Please note that we do not call setFailure while a lock is
                // acquired, to avoid a potential dead lock.
                for (;;) {
                    PendingWrite pendingWrite;
                    synchronized (pendingUnencryptedWrites) {
                        pendingWrite = pendingUnencryptedWrites.poll();
                        if (pendingWrite == null) {
                            break;
                        }
                    }

                    pendingWrite.future.setFailure(cause);
                }
            }
        }

        if (needsUnwrap) {
            unwrap(context, channel, ChannelBuffers.EMPTY_BUFFER, 0, 0);
        }

        if (future == null) {
            future = succeededFuture(channel);
        }
        return future;
    }

    private void offerEncryptedWriteRequest(MessageEvent encryptedWrite) {
        boolean offered;
        if (Thread.holdsLock(pendingEncryptedWrites)) {
            offered = pendingEncryptedWrites.offer(encryptedWrite);
        } else {
            synchronized (pendingEncryptedWrites) {
                offered = pendingEncryptedWrites.offer(encryptedWrite);
            }
        }
        assert offered;
    }

    private void flushPendingEncryptedWrites(ChannelHandlerContext ctx) {
        // Avoid possible dead lock and data integrity issue
        // which is caused by cross communication between more than one channel
        // in the same VM.
        if (Thread.holdsLock(pendingEncryptedWrites)) {
            return;
        }

        synchronized (pendingEncryptedWrites) {
            if (pendingEncryptedWrites.isEmpty()) {
                return;
            }
        }

        synchronized (pendingEncryptedWrites) {
            MessageEvent e;
            while ((e = pendingEncryptedWrites.poll()) != null) {
                ctx.sendDownstream(e);
            }
        }
    }

    private ChannelFuture wrapNonAppData(ChannelHandlerContext ctx, Channel channel) throws SSLException {
        ChannelFuture future = null;
        ByteBuffer outNetBuf = bufferPool.acquire();

        SSLEngineResult result;
        try {
            for (;;) {
                synchronized (handshakeLock) {
                    result = engine.wrap(EMPTY_BUFFER, outNetBuf);
                }

                if (result.bytesProduced() > 0) {
                    outNetBuf.flip();
                    ChannelBuffer msg = ChannelBuffers.buffer(outNetBuf.remaining());
                    msg.writeBytes(outNetBuf.array(), 0, msg.capacity());
                    outNetBuf.clear();

                    future = future(channel);
                    future.addListener(new ChannelFutureListener() {
                        public void operationComplete(ChannelFuture future)
                                throws Exception {
                            if (future.getCause() instanceof ClosedChannelException) {
                                synchronized (ignoreClosedChannelExceptionLock) {
                                    ignoreClosedChannelException ++;
                                }
                            }
                        }
                    });

                    write(ctx, future, msg);
                }

                switch (result.getHandshakeStatus()) {
                case FINISHED:
                    setHandshakeSuccess(channel);
                    runDelegatedTasks();
                    break;
                case NEED_TASK:
                    runDelegatedTasks();
                    break;
                case NEED_UNWRAP:
                    if (!Thread.holdsLock(handshakeLock)) {
                        // unwrap shouldn't be called when this method was
                        // called by unwrap - unwrap will keep running after
                        // this method returns.
                        unwrap(ctx, channel, ChannelBuffers.EMPTY_BUFFER, 0, 0);
                    }
                    break;
                case NOT_HANDSHAKING:
                case NEED_WRAP:
                    break;
                default:
                    throw new IllegalStateException(
                            "Unexpected handshake status: " +
                            result.getHandshakeStatus());
                }

                if (result.bytesProduced() == 0) {
                    break;
                }
            }
        } catch (SSLException e) {
            setHandshakeFailure(channel, e);
            throw e;
        } finally {
            bufferPool.release(outNetBuf);
        }

        if (future == null) {
            future = succeededFuture(channel);
        }

        return future;
    }

    private ChannelBuffer unwrap(
            ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer, int offset, int length) throws SSLException {
        ByteBuffer inNetBuf = buffer.toByteBuffer(offset, length);
        ByteBuffer outAppBuf = bufferPool.acquire();

        try {
            boolean needsWrap = false;
            loop:
            for (;;) {
                SSLEngineResult result;
                synchronized (handshakeLock) {
                    if (initialHandshake && !engine.getUseClientMode() &&
                        !engine.isInboundDone() && !engine.isOutboundDone()) {
                        handshake(channel);
                        initialHandshake = false;
                    }

                    try {
                        result = engine.unwrap(inNetBuf, outAppBuf);
                    } catch (SSLException e) {
                        throw e;
                    }

                    switch (result.getHandshakeStatus()) {
                    case NEED_UNWRAP:
                        if (inNetBuf.hasRemaining() && !engine.isInboundDone()) {
                            break;
                        } else {
                            break loop;
                        }
                    case NEED_WRAP:
                        wrapNonAppData(ctx, channel);
                        break;
                    case NEED_TASK:
                        runDelegatedTasks();
                        break;
                    case FINISHED:
                        setHandshakeSuccess(channel);
                        needsWrap = true;
                        break loop;
                    case NOT_HANDSHAKING:
                        needsWrap = true;
                        break loop;
                    default:
                        throw new IllegalStateException(
                                "Unknown handshake status: " +
                                result.getHandshakeStatus());
                    }
                }
            }

            if (needsWrap) {
                // wrap() acquires pendingUnencryptedWrites first and then
                // handshakeLock.  If handshakeLock is already hold by the
                // current thread, calling wrap() will lead to a dead lock
                // i.e. pendingUnencryptedWrites -> handshakeLock vs.
                //      handshakeLock -> pendingUnencryptedLock -> handshakeLock
                //
                // There is also a same issue between pendingEncryptedWrites
                // and pendingUnencryptedWrites.
                if (!Thread.holdsLock(handshakeLock) &&
                    !Thread.holdsLock(pendingEncryptedWrites)) {
                    wrap(ctx, channel);
                }
            }

            outAppBuf.flip();

            if (outAppBuf.hasRemaining()) {
                ChannelBuffer frame = ChannelBuffers.buffer(outAppBuf.remaining());
                frame.writeBytes(outAppBuf.array(), 0, frame.capacity());
                return frame;
            } else {
                return null;
            }
        } catch (SSLException e) {
            setHandshakeFailure(channel, e);
            throw e;
        } finally {
            bufferPool.release(outAppBuf);
        }
    }

    private void runDelegatedTasks() {
        for (;;) {
            final Runnable task;
            synchronized (handshakeLock) {
                task = engine.getDelegatedTask();
            }

            if (task == null) {
                break;
            }

            delegatedTaskExecutor.execute(new Runnable() {
                public void run() {
                    synchronized (handshakeLock) {
                        task.run();
                    }
                }
            });
        }
    }

    private void setHandshakeSuccess(Channel channel) {
        synchronized (handshakeLock) {
            handshaking = false;
            handshaken = true;

            if (handshakeFuture == null) {
                handshakeFuture = future(channel);
            }
        }

        handshakeFuture.setSuccess();
    }

    private void setHandshakeFailure(Channel channel, SSLException cause) {
        synchronized (handshakeLock) {
            if (!handshaking) {
                return;
            }
            handshaking = false;
            handshaken = false;

            if (handshakeFuture == null) {
                handshakeFuture = future(channel);
            }
        }
        handshakeFuture.setFailure(cause);
    }

    private void closeOutboundAndChannel(
            final ChannelHandlerContext context, final ChannelStateEvent e) throws SSLException {
        if (!e.getChannel().isConnected()) {
            context.sendDownstream(e);
            return;
        }

        unwrap(context, e.getChannel(), ChannelBuffers.EMPTY_BUFFER, 0, 0);
        if (!engine.isInboundDone()) {
            if (sentCloseNotify.compareAndSet(false, true)) {
                engine.closeOutbound();
                ChannelFuture closeNotifyFuture = wrapNonAppData(context, e.getChannel());
                closeNotifyFuture.addListener(
                        new ClosingChannelFutureListener(context, e));
                return;
            }
        }

        context.sendDownstream(e);
    }

    private static final class PendingWrite {
        final ChannelFuture future;
        final ByteBuffer outAppBuf;

        PendingWrite(ChannelFuture future, ByteBuffer outAppBuf) {
            this.future = future;
            this.outAppBuf = outAppBuf;
        }
    }

    private static final class ClosingChannelFutureListener implements ChannelFutureListener {

        private final ChannelHandlerContext context;
        private final ChannelStateEvent e;

        ClosingChannelFutureListener(
                ChannelHandlerContext context, ChannelStateEvent e) {
            this.context = context;
            this.e = e;
        }

        public void operationComplete(ChannelFuture closeNotifyFuture) throws Exception {
            if (!(closeNotifyFuture.getCause() instanceof ClosedChannelException)) {
                Channels.close(context, e.getFuture());
            }
        }
    }
}
