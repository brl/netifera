package com.netifera.platform.net.tools.portscanning;

import java.util.BitSet;
import java.util.Map;
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
import com.netifera.platform.util.locators.TCPSocketLocator;

public class TCPConnectScanner extends AbstractPortscanner {	
//	private int errorCount = 0;
//	private int errorThreshold = 30;
	private boolean skipUnreachable = true;
	private int maximumConnections = 250;
	
	final private AtomicInteger connectionsCount = new AtomicInteger(0);
	private BitSet badHostSet;

	private int firstPort;
	
	private ChannelFactory factory;
	private Timer timer;

	@Override
	protected void scannerRun() {
		int hostCount = targetNetwork.size();
		badHostSet = new BitSet(hostCount);
		
		firstPort = targetPorts.contains(80) ? 80 : targetPorts.get(0);
		context.setTitle("TCP connect scan "+targetNetwork);
		context.setStatus("Scanning port "+firstPort);
		context.setTotalWork(targetNetwork.size()*targetPorts.size());
		
//		context.enableDebugOutput();

		factory = new NioClientSocketChannelFactory(
                Executors.newCachedThreadPool(),
                Executors.newCachedThreadPool());
		
		timer = new HashedWheelTimer();
		
		try {
			try {
				context.info("Scanning port "+firstPort);
				if (scanFirstPort(firstPort) == false)
					return;
	
				if (targetPorts.size() > 1) {
					context.info("Scanning the rest of the ports");
					for (int i = 0; i < targetNetwork.size(); i++)
						if (scanHost(i) == false)
							return;
				}
			} catch (InterruptedException e) {
				context.warning("Interrupted");
				context.setStatus("Cancelling "+connectionsCount.get()+" connections");
				Thread.currentThread().interrupt();
				return;
			} catch (Exception e) {
				context.exception("Exception", e);
			}
			while (connectionsCount.get() > 0) {
				context.setStatus("Outstanding connections "+connectionsCount.get());
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

	private boolean scanFirstPort(final int port) throws InterruptedException {
		for (int i = 0; i < targetNetwork.size(); i++) {
			while (connectionsCount.get() >= maximumConnections && !Thread.currentThread().isInterrupted()) {
				Thread.sleep(100);
			}
			if (Thread.currentThread().isInterrupted())
				return false;
			final TCPSocketLocator locator = new TCPSocketLocator(targetNetwork.get(i),port);
/*			try {
*/				final int index = i;
				final TCPConnectServiceDetector detector = new TCPConnectServiceDetector(locator, timer, factory, context.getLogger());
				detector.detect(new ITCPConnectServiceDetectorListener() {
					public void connecting(TCPSocketLocator locator) {
						connectionsCount.incrementAndGet();
					}

					public void badTarget(final TCPSocketLocator locator) {
						if (markTargetBad(index)) {
							context.worked(targetPorts.size()-1); //XXX is this number ok? might not be the first port we scan
							context.debug("Skipping unreachable host "+locator.getAddress());
						}
					}

					public void connected(TCPSocketLocator locator) {
						PortSet ports = new PortSet();
						ports.addPort(locator.getPort());
						Activator.getInstance().getNetworkEntityFactory().addOpenTCPPorts(context.getRealm(), context.getSpaceId(), locator.getAddress(), ports);
					}

					public void serviceDetected(TCPSocketLocator locator,
							Map<String, String> info) {
						Activator.getInstance().getNetworkEntityFactory().createService(context.getRealm(), context.getSpaceId(), locator, info.get("serviceType"), info);
					}
					
					public void finished(TCPSocketLocator locator) {
						connectionsCount.decrementAndGet();
						context.worked(1);
					}
				});
				waitDelay();
/*			} catch (PortUnreachableException e) {
				continue;
			} catch (SocketException e) {
				markTargetBad(i, new Runnable() {
					public void run() {
						context.worked(targetPorts.size()-1); // remaining ports
						context.debug("Skipping unreachable host " + locator.getAddress());
					}
				});
			} catch (IOException e) {
				context.debug("Connecting to " + locator + " failed with error " + e);
				errorCount++;
				if (errorCount >= errorThreshold) {
					context.error("Too many errors, aborting.");
					return false;
				}
			}
*/		}
		return true;
	}

	private boolean scanHost(final int index) throws InterruptedException {
		final InternetAddress target = targetNetwork.get(index);
		context.setStatus("Scanning host "+target);
		
		for (int i = 0; i < targetPorts.size(); i++) {
			if (isTargetBad(index)) {
				return true;
			}

			int port = targetPorts.get(i);
			if (port == firstPort)
				continue; // already scanned before

			while (connectionsCount.get() >= maximumConnections && !Thread.currentThread().isInterrupted()) {
				Thread.sleep(100);
			}
			if (Thread.currentThread().isInterrupted())
				return false;

			final TCPSocketLocator locator = new TCPSocketLocator(target, port);

//			try {
				final TCPConnectServiceDetector detector = new TCPConnectServiceDetector(locator, timer, factory, context.getLogger());
				detector.detect(new ITCPConnectServiceDetectorListener() {

					public void connecting(TCPSocketLocator locator) {
						connectionsCount.incrementAndGet();
					}

					public void badTarget(final TCPSocketLocator locator) {
						if (markTargetBad(index)) {
							context.worked(targetPorts.size()-1); //XXX is this number ok? might not be the first port we scan
							context.debug("Skipping unreachable host "+locator.getAddress()/*+", "+e.getMessage()*/);
						}
					}

					public void connected(TCPSocketLocator locator) {
						PortSet ports = new PortSet();
						ports.addPort(locator.getPort());
						Activator.getInstance().getNetworkEntityFactory().addOpenTCPPorts(context.getRealm(), context.getSpaceId(), locator.getAddress(), ports);
					}
					
					public void serviceDetected(TCPSocketLocator locator,
							Map<String, String> info) {
						Activator.getInstance().getNetworkEntityFactory().createService(context.getRealm(), context.getSpaceId(), locator, info.get("serviceType"), info);
					}


					public void finished(TCPSocketLocator locator) {
						connectionsCount.decrementAndGet();
						context.worked(1);
					}
				});
				waitDelay();
/*			} catch (PortUnreachableException e) {
				continue;
			} catch (final SocketException e) {
				final int remainingPorts = targetPorts.size()-i-1;
				markTargetBad(index, new Runnable() {
					public void run() {
						context.worked(remainingPorts);
						context.debug("Skipping unreachable host "+target+", "+e.getMessage());
					}
				});
				return true;
			} catch (IOException e) {
				context.debug("Connecting to " + locator + " failed, "+e.getMessage());
				errorCount++;
				if (errorCount >= errorThreshold) {
					context.error("Too many errors, aborting.");
					return false;
				}
			}
*/		}
		return true;
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
		synchronized (badHostSet) {
			return badHostSet.get(index);
		}
	}

	protected void setupToolOptions() throws ToolException {
		context.setTitle("TCP Connect Scan");
		if (context.getConfiguration().get("skipUnreachable") != null)
			skipUnreachable = (Boolean) context.getConfiguration().get("skipUnreachable");
		if (context.getConfiguration().get("maximumConnections") != null)
			maximumConnections = (Integer) context.getConfiguration().get("maximumConnections");
		super.setupToolOptions();
	}
}
