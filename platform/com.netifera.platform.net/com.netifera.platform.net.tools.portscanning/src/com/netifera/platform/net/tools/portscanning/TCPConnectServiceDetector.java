package com.netifera.platform.net.tools.portscanning;

import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
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
import org.jboss.netty.handler.timeout.ReadTimeoutException;
import org.jboss.netty.handler.timeout.ReadTimeoutHandler;
import org.jboss.netty.handler.timeout.WriteTimeoutHandler;
import org.jboss.netty.util.Timer;

import com.netifera.platform.api.log.ILogger;
import com.netifera.platform.net.internal.tools.portscanning.Activator;
import com.netifera.platform.util.locators.TCPSocketLocator;

public class TCPConnectServiceDetector {

	public final static int CONNECT_TIMEOUT = 5000;
	public final static int READ_BANNER_TIMEOUT = 100;
	public final static int WRITE_TIMEOUT = 5000;
	public final static int READ_RESPONSE_TIMEOUT = 10000;
	public final static int SHORT_READ_TIMEOUT = READ_BANNER_TIMEOUT;
	
	private final TCPSocketLocator locator;
	
	private final ChannelFactory factory;
	private final Timer timer;
	private final ILogger logger;
	
	private ITCPConnectServiceDetectorListener listener;

	private volatile Map<String,String> serviceInfo = null;

	TCPConnectServiceDetector(TCPSocketLocator locator, Timer timer, ChannelFactory factory, ILogger logger) {
		this.locator = locator;
		this.timer = timer;
		this.factory = factory;
		this.logger = logger;
	}
	
	public void detect(final ITCPConnectServiceDetectorListener listener) {
		this.listener = listener;
		
		ClientBootstrap bootstrap = new ClientBootstrap(factory);
		bootstrap.setPipelineFactory(createPipelineFactory());
		
		bootstrap.setOption("tcpNoDelay", true);
		bootstrap.setOption("keepAlive", true);
		bootstrap.setOption("connectTimeoutMillis", CONNECT_TIMEOUT);

		listener.connecting(locator);
		try {
			bootstrap.connect(locator.toInetSocketAddress());
		} catch (ChannelException e) {
			listener.finished(locator);
			throw e;
		}
	}

	private ChannelPipelineFactory createPipelineFactory() {
		return new ChannelPipelineFactory() {
			public ChannelPipeline getPipeline() throws Exception {
				ChannelPipeline pipeline = Channels.pipeline();
				pipeline.addLast("handler", new TCPServiceDetectionHandler());
				return pipeline;
			}
		};
	}

	@ChannelPipelineCoverage("one")
	class TCPServiceDetectionHandler extends SimpleChannelHandler {
		boolean triggerWritten = false;
		final ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
		ByteBuffer trigger;

		@Override
		public void channelConnected(
				ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
			listener.connected(locator);
			
			ChannelPipeline pipeline = e.getChannel().getPipeline();
			pipeline.addBefore("handler", "readTimeout", new ReadTimeoutHandler(timer, READ_BANNER_TIMEOUT, TimeUnit.MILLISECONDS));
			pipeline.addBefore("handler", "writeTimeout", new WriteTimeoutHandler(timer, WRITE_TIMEOUT, TimeUnit.MILLISECONDS));
		}

		@Override
		public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
			buffer.writeBytes((ChannelBuffer)e.getMessage());
			serviceInfo = Activator.getInstance().getServerDetector().detect("tcp", locator.getPort(), trigger, buffer.toByteBuffer());
			if (serviceInfo != null) {
				listener.serviceDetected(locator, serviceInfo);
				e.getChannel().close();
			} else {
				if (!triggerWritten) {
					writeTrigger(e.getChannel());
				} else {
					// unrecognized service
					checkUnrecognized();
					e.getChannel().close();
				}
			}
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
//			e.getCause().printStackTrace();
			
			if (e.getCause() instanceof ReadTimeoutException && !triggerWritten) {
				writeTrigger(e.getChannel());
				return;
			}

			// ConnectException = closed or rejected
			if (e.getCause() instanceof ConnectException) {
				e.getChannel().close();
				return;
			}
			
			if (e.getCause() instanceof NoRouteToHostException || e.getCause() instanceof SocketException) {
//				logger.debug("Bad target: "+locator, e.getCause());
				listener.badTarget(locator);
			} else {
				logger.error("Unexpected exception when scanning "+locator, e.getCause());
			}
			e.getChannel().close();
		}

		@Override
		public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
			if (serviceInfo != null)
				logger.info(serviceInfo.get("serviceType")+" @ "+locator);
			listener.finished(locator);
		}

		private void writeTrigger(Channel channel) {
			triggerWritten = true;
			byte[] triggerBytes = Activator.getInstance().getServerDetector().getTrigger("tcp",locator.getPort());
			trigger = ByteBuffer.wrap(triggerBytes);
			if (triggerBytes.length > 0)
				channel.write(ChannelBuffers.wrappedBuffer(trigger));
			channel.getPipeline().replace("readTimeout", "readTimeout", new ReadTimeoutHandler(timer, READ_RESPONSE_TIMEOUT, TimeUnit.MILLISECONDS));
		}
			 	
	 	private void checkUnrecognized() {
	 		if (serviceInfo == null)
	 			logger.warning("Unrecognized service @ " + locator);
	 	}
	}
}
