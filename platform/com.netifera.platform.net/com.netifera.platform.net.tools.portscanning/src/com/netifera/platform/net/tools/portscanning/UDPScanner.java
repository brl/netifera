package com.netifera.platform.net.tools.portscanning;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ConnectionlessBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.FixedReceiveBufferSizePredictorFactory;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.socket.DatagramChannel;
import org.jboss.netty.channel.socket.DatagramChannelFactory;
import org.jboss.netty.channel.socket.oio.OioDatagramChannelFactory;

import com.netifera.platform.api.tools.ToolException;
import com.netifera.platform.net.internal.tools.portscanning.Activator;
import com.netifera.platform.tools.RequiredOptionMissingException;
import com.netifera.platform.util.PortSet;
import com.netifera.platform.util.addresses.inet.InternetAddress;
import com.netifera.platform.util.locators.UDPSocketLocator;


public class UDPScanner extends AbstractPortscanner {
	private Integer timeout;

	@Override
	protected void setupToolOptions() throws ToolException {
		context.setTitle("UDP scan");
		super.setupToolOptions();
		timeout = (Integer) context.getConfiguration().get("timeout");
		if (timeout == null)
			throw new RequiredOptionMissingException("timeout");
	}
	
	@Override
	protected void scannerRun() throws ToolException {
		context.setTitle("UDP scan "+targetNetwork);
		context.setTotalWork(targetNetwork.size()*targetPorts.size()+1); //+1 in order to account for waiting responses after sending all requests

		DatagramChannelFactory factory =
			new OioDatagramChannelFactory(Executors.newCachedThreadPool());
		
		ConnectionlessBootstrap bootstrap = new ConnectionlessBootstrap(factory);

		// Configure the pipeline.
		ChannelPipeline pipeline = bootstrap.getPipeline();
		pipeline.addLast("handler", new UDPScannerChannelHandler());
		
		// Allow packets as large as up to 4096 bytes (default is 768).
		// You could increase or decrease this value to avoid truncated packets
		// or to improve memory footprint respectively.
		bootstrap.setOption(
				"receiveBufferSizePredictorFactory",
				new FixedReceiveBufferSizePredictorFactory(4096));

		DatagramChannel channel = null;
		
		try {
			channel = (DatagramChannel) bootstrap.bind(new InetSocketAddress(0));

			scanAllAddresses(channel);

/*			// If the channel is not closed within 5 seconds,
			// print an error message and quit.
			if (!channel.getCloseFuture().awaitUninterruptibly(5000)) {
				System.err.println("QOTM request timed out.");
				channel.close().awaitUninterruptibly();
			}
*/
			context.setStatus("Waiting responses for "+timeout+" seconds...");
			Thread.sleep(timeout*1000L);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			context.warning("Interrupted");
			return;
		} finally {
			if (channel != null)
				channel.close().awaitUninterruptibly();
			factory.releaseExternalResources();
		}
	}

	private void scanAllAddresses(DatagramChannel channel) throws InterruptedException {
		ChannelBuffer writeBuffer = ChannelBuffers.buffer(4096);
		for (Integer port: targetPorts) {
			byte[] trigger = Activator.getInstance().getServerDetector().getTrigger("udp",port);
//			context.debug("Trigger for port "+port+": "+trigger);
			writeBuffer.clear();
			writeBuffer.writeBytes(trigger);
			context.setStatus("Scanning "+targetNetwork+":"+port+"/udp");
			for (InternetAddress address: targetNetwork) {
				if (Thread.currentThread().isInterrupted())
					throw new InterruptedException();
				try {
					writeBuffer.resetReaderIndex();
					ChannelFuture future = channel.write(writeBuffer, new InetSocketAddress(address.toInetAddress(), port));
					future.await();
					waitDelay();
				} finally {
					context.worked(1);
				}
			}
		}
	}

	@ChannelPipelineCoverage("all")
	class UDPScannerChannelHandler extends SimpleChannelUpstreamHandler {

		@Override
		public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
				throws Exception {
			UDPSocketLocator peer = new UDPSocketLocator((InetSocketAddress)e.getRemoteAddress());
			PortSet ports = new PortSet();
			ports.addPort(peer.getPort());
			Activator.getInstance().getNetworkEntityFactory().addOpenUDPPorts(context.getRealm(), context.getSpaceId(), peer.getAddress(), ports);
			byte[] trigger = Activator.getInstance().getServerDetector().getTrigger("udp",peer.getPort());
			ByteBuffer responseBuffer = ((ChannelBuffer)e.getMessage()).toByteBuffer();
			Map<String,String> serviceInfo = Activator.getInstance().getServerDetector().detect("udp", peer.getPort(), ByteBuffer.wrap(trigger), responseBuffer);
			if (serviceInfo != null) {
				Activator.getInstance().getNetworkEntityFactory().createService(context.getRealm(), context.getSpaceId(), peer, serviceInfo.get("serviceType"), serviceInfo);
				context.info(serviceInfo.get("serviceType")+" @ "+peer);
			} else {
				context.warning("Unknown service @ " + peer);
			}
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
				throws Exception {
			e.getCause().printStackTrace();
			e.getChannel().close();
		}
	}
}
