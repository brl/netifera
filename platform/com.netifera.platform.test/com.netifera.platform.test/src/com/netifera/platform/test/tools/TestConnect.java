package com.netifera.platform.test.tools;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.ChannelException;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.timeout.ReadTimeoutHandler;
import org.jboss.netty.handler.timeout.WriteTimeoutHandler;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timer;

import com.netifera.platform.api.tools.ITool;
import com.netifera.platform.api.tools.IToolContext;
import com.netifera.platform.api.tools.ToolException;
import com.netifera.platform.util.addresses.inet.TCPSocketAddress;

public class TestConnect implements ITool {
	
	private IToolContext context;
	private TCPSocketAddress target;
	
	private ChannelFactory factory;
	private Timer timer;

	private int connectTimeout;
	private int numberOfConnections;
	private int delayBetweenConnections;
	
	private void setup() throws ToolException {
		context.setTitle("Test connect()");
		target = (TCPSocketAddress) context.getConfiguration().get("target");
		connectTimeout = (Integer) context.getConfiguration().get("connectTimeout");
		numberOfConnections = (Integer) context.getConfiguration().get("numberOfConnections");
		delayBetweenConnections = (Integer) context.getConfiguration().get("delayBetweenConnections");
	}
	
	public void run(IToolContext context) throws ToolException {
		this.context = context;
		setup();
		
		context.setTitle("Test connect to "+target);

		context.enableDebugOutput();
		
		factory = new NioClientSocketChannelFactory(
                Executors.newCachedThreadPool(),
                Executors.newCachedThreadPool());
		
		timer = new HashedWheelTimer();

		try {
			ClientBootstrap bootstrap = new ClientBootstrap(factory);
			bootstrap.setPipelineFactory(createPipelineFactory());
			
			bootstrap.setOption("tcpNoDelay", true);
			bootstrap.setOption("keepAlive", true);
			bootstrap.setOption("connectTimeoutMillis", connectTimeout);

			for (int i=0; i<numberOfConnections; i++) {
				try {
					System.err.println("CONNECTING "+i);
					bootstrap.connect(target.toInetSocketAddress());
					Thread.sleep(delayBetweenConnections);
				} catch (ChannelException e) {
					e.printStackTrace();
					throw e;
				}
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			context.warning("Interrupted");
		} finally {
			System.err.println("STOPPING TIMER");
			timer.stop();
			System.err.println("RELEASING CHANNEL FACTORY RESOURCES");
			factory.releaseExternalResources();
			System.err.println("DONE");
		}
	}

	private ChannelPipelineFactory createPipelineFactory() {
		return new ChannelPipelineFactory() {
			public ChannelPipeline getPipeline() throws Exception {
				ChannelPipeline pipeline = Channels.pipeline();
				pipeline.addLast("handler", new TestHandler());
				return pipeline;
			}
		};
	}

	@ChannelPipelineCoverage("one")
	class TestHandler extends SimpleChannelHandler {

		long startTime = System.currentTimeMillis();
			
		@Override
		public void channelConnected(
				ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
			print("CONNECTED "+ctx.getChannel());
			
			ChannelPipeline pipeline = e.getChannel().getPipeline();
			pipeline.addBefore("handler", "readTimeout", new ReadTimeoutHandler(timer, 1000, TimeUnit.MILLISECONDS));
			pipeline.addBefore("handler", "writeTimeout", new WriteTimeoutHandler(timer, 1000, TimeUnit.MILLISECONDS));
		}

		@Override
		public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
			print("RECEIVED DATA "+ctx.getChannel());
			e.getChannel().close();
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
			print("EXCEPTION");
			e.getCause().printStackTrace();
			e.getChannel().close();
		}

		@Override
		public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
			print("CLOSED "+ctx.getChannel());
		}
		
		private void print(String message) {
			long msecs = System.currentTimeMillis() - startTime;
			System.err.println(msecs+" "+message);
		}
	}

}
