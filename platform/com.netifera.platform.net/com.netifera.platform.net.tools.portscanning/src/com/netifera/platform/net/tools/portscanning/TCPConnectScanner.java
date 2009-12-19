package com.netifera.platform.net.tools.portscanning;

import java.util.BitSet;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timer;

import com.netifera.platform.api.tools.ToolException;
import com.netifera.platform.net.internal.tools.portscanning.Activator;
import com.netifera.platform.util.PortSet;
import com.netifera.platform.util.addresses.inet.InternetAddress;
import com.netifera.platform.util.addresses.inet.TCPSocketAddress;

public class TCPConnectScanner extends AbstractPortscanner {	
	private boolean skipUnreachable = true;
	private int maximumConnections = 250;
	
	final private AtomicInteger connectionsCount = new AtomicInteger(0);
	private BitSet badHostSet;

	private boolean randomize = false;
	
	private ChannelFactory factory;
	private Timer timer;

	@Override
	protected void scannerRun() {
		if (skipUnreachable) {
			badHostSet = new BitSet(targetNetwork.size());
		}
		
//		context.enableDebugOutput();

		factory = new NioClientSocketChannelFactory(
                Executors.newCachedThreadPool(),
                Executors.newCachedThreadPool());
		
		timer = new HashedWheelTimer();
		
		try {
			try {
				if (!randomize) {
					int firstPort = targetPorts.contains(80) ? 80 : targetPorts.get(0);
					context.setTitle("TCP connect scan "+targetNetwork);
					context.setTotalWork(targetNetwork.size()*targetPorts.size());
					context.info("Scanning port "+firstPort);
					scanFirstPort(firstPort);
		
					if (targetPorts.size() > 1) {
						context.info("Scanning the rest of the ports");
						for (int i = 0; i < targetNetwork.size(); i++)
							scanRemainingPorts(i, firstPort);
					}
				} else {
					context.setTitle("TCP connect random scan "+targetNetwork);
					context.info("Randomly scanning "+targetNetwork);
					randomScan();
				}
			} catch (InterruptedException e) {
				context.warning("Interrupted");
				context.setSubTitle("Cancelling "+connectionsCount.get()+" connections");
				Thread.currentThread().interrupt();
				return;
			} catch (Exception e) {
				context.exception("Exception", e);
			}
			while (connectionsCount.get() > 0) {
				context.setSubTitle("Waiting "+connectionsCount.get()+" outstanding connections");
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					context.warning("Interrupted");
					Thread.currentThread().interrupt();
					return;
				}
			}
		} finally {
			timer.stop();
			factory.releaseExternalResources();
		}
	}

	/*
	 * Scan the first port on the given host, and if unreachable hosts
	 * are chosen to be skipped it will mark them as bad targets so they
	 * wont be scanned on the next step.
	 * 
	 * Return true if the scan is intended to continue, false if it should stop (if it was interrupted)
	 */
	private void scanFirstPort(final int port) throws InterruptedException {
		for (int i = 0; i < targetNetwork.size(); i++) {
			scan(i, port, true);
			waitDelay();
		}
	}

	/*
	 * Scan the rest of the ports (all except the first port, which was already scanned) on the given host.
	 * Return true if the scan is intended to continue, false if it should stop (if it was interrupted)
	 */
	private void scanRemainingPorts(final int index, int firstPort) throws InterruptedException {
		final InternetAddress target = targetNetwork.get(index);
		context.setSubTitle("Scanning host "+target);
		
		for (int i = 0; i < targetPorts.size(); i++) {
			if (isTargetBad(index))
				return;

			int port = targetPorts.get(i);
			if (port == firstPort)
				continue; // already scanned before

			scan(index, port, false); // dont mark unreachable targets as bad in this step
			waitDelay();
		}
	}

	private void randomScan() throws InterruptedException {
		Random random = new Random(System.currentTimeMillis());
		
		while (true) {
			if (Thread.currentThread().isInterrupted())
				throw new InterruptedException();
			int index = random.nextInt(targetNetwork.size());
			if (isTargetBad(index))
				continue;
			int port = targetPorts.get(random.nextInt(targetPorts.size()));
			scan(index, port, true);
			waitDelay();
		}
	}
	
	private void scan(final int index, int port, final boolean markBadTargets) throws InterruptedException {
		while (connectionsCount.get() >= maximumConnections && !Thread.currentThread().isInterrupted()) {
			Thread.sleep(100);
		}
		if (Thread.currentThread().isInterrupted())
			throw new InterruptedException();
		
		final TCPSocketAddress socketAddress = new TCPSocketAddress(targetNetwork.get(index),port);
		final TCPConnectServiceDetector detector = new TCPConnectServiceDetector(socketAddress, timer, factory, context.getLogger());
		detector.detect(new ITCPConnectServiceDetectorListener() {
			public void connecting(TCPSocketAddress socketAddress) {
				connectionsCount.incrementAndGet();
			}

			public void unreachable(TCPSocketAddress socketAddress) {
				if (markBadTargets) {
					if (markTargetBad(index)) {
						context.worked(targetPorts.size()-1); //XXX here we're assuming unreachable hosts are detected on the first port scanned
						context.debug("Skipping unreachable host "+socketAddress.getNetworkAddress());
					}
				}
			}

			public void connected(TCPSocketAddress socketAddress) {
				PortSet ports = new PortSet();
				ports.addPort(socketAddress.getPort());
				Activator.getInstance().getNetworkEntityFactory().addOpenTCPPorts(context.getRealm(), context.getSpaceId(), socketAddress.getNetworkAddress(), ports);
			}

			public void serviceDetected(TCPSocketAddress socketAddress,
					Map<String, String> info) {
				Activator.getInstance().getNetworkEntityFactory().createService(context.getRealm(), context.getSpaceId(), socketAddress, info.get("serviceType"), info);
			}
			
			public void finished(TCPSocketAddress socketAddress) {
				connectionsCount.decrementAndGet();
				context.worked(1);
			}
		});
	}
	
	private boolean markTargetBad(int index) {
		if (skipUnreachable) {
			synchronized (badHostSet) {
				if (!badHostSet.get(index)) {
					badHostSet.set(index);
					return true;
				}
			}
		}
		return false;
	}

	private boolean isTargetBad(int index) {
		if (skipUnreachable) {
			synchronized (badHostSet) {
				return badHostSet.get(index);
			}
		}
		return false;
	}

	protected void setupToolOptions() throws ToolException {
		context.setTitle("TCP Connect Scan");
		if (context.getConfiguration().get("skipUnreachable") != null)
			skipUnreachable = (Boolean) context.getConfiguration().get("skipUnreachable");
		if (context.getConfiguration().get("maximumConnections") != null)
			maximumConnections = (Integer) context.getConfiguration().get("maximumConnections");
		if (context.getConfiguration().get("randomize") != null)
			randomize = (Boolean) context.getConfiguration().get("randomize");
		super.setupToolOptions();
	}
}
