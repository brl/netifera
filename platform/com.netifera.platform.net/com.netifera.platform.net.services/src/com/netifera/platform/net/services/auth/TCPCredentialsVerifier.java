package com.netifera.platform.net.services.auth;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.ChannelException;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.socket.ClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.timeout.ReadTimeoutHandler;
import org.jboss.netty.handler.timeout.WriteTimeoutHandler;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timer;

import com.netifera.platform.util.addresses.inet.TCPSocketAddress;

public abstract class TCPCredentialsVerifier extends CredentialsVerifier {
	public final static int CONNECT_TIMEOUT = 10000;
	public final static int READ_TIMEOUT = 5000;
	public final static int WRITE_TIMEOUT = 5000;
	
	final protected TCPSocketAddress target;
	private int maximumConnections = 10;
	
	final private AtomicInteger connectionsCount = new AtomicInteger(0);

	private ClientSocketChannelFactory channelFactory;
	private ClientBootstrap bootstrap;
	private Timer timer;

	public TCPCredentialsVerifier(TCPSocketAddress target) {
		this.target = target;
	}

	public void setMaximumConnections(int maximumConnections) {
		this.maximumConnections = maximumConnections;
	}

	@Override
	public void run() throws IOException, InterruptedException {
		try {
			channelFactory = new NioClientSocketChannelFactory(
					Executors.newCachedThreadPool(),
					Executors.newCachedThreadPool());
			timer = new HashedWheelTimer();

			bootstrap = new ClientBootstrap(channelFactory);
			bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
				public ChannelPipeline getPipeline() throws Exception {
					ChannelPipeline pipeline = TCPCredentialsVerifier.this.createPipeline();
					pipeline.addLast("connectAndCloseHandler", new ConnectAndCloseHandler());
					return pipeline;
				}
			});
			
			bootstrap.setOption("tcpNoDelay", true);
			bootstrap.setOption("keepAlive", true);
			bootstrap.setOption("connectTimeoutMillis", CONNECT_TIMEOUT);
			
			while ((hasNextCredential() || connectionsCount.get() > 0) && !Thread.currentThread().isInterrupted()) {
				while (connectionsCount.get() >= maximumConnections) {
					Thread.sleep(100);
				}
				if (hasNextCredential() && !Thread.currentThread().isInterrupted()) spawnConnection();
			}
		} finally {
			if (timer != null) timer.stop();
			if (channelFactory != null) channelFactory.releaseExternalResources();
		}
	}

	private void spawnConnection() {
		connectionsCount.incrementAndGet();
		try {
			bootstrap.connect(target.toInetSocketAddress());
		} catch (ChannelException e) {
			connectionsCount.decrementAndGet();
			throw e;
		}
	}

	protected abstract ChannelPipeline createPipeline() throws Exception;
	
	@ChannelPipelineCoverage("all")
	class ConnectAndCloseHandler extends SimpleChannelHandler {
		
		@Override
		public void channelConnected(
				ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
			ChannelPipeline pipeline = e.getChannel().getPipeline();
			pipeline.addBefore("handler", "readTimeout", new ReadTimeoutHandler(timer, READ_TIMEOUT, TimeUnit.MILLISECONDS));
			pipeline.addBefore("handler", "writeTimeout", new WriteTimeoutHandler(timer, WRITE_TIMEOUT, TimeUnit.MILLISECONDS));
		}

		@Override
	    public void channelClosed(
	            ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
	    	connectionsCount.decrementAndGet();
	    }
	}
}
