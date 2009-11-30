package com.netifera.platform.net.tools.portscanning;

import java.io.IOException;
import java.net.PortUnreachableException;
import java.net.SocketException;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.netifera.platform.api.tools.ToolException;
import com.netifera.platform.net.internal.tools.portscanning.Activator;
import com.netifera.platform.util.PortSet;
import com.netifera.platform.util.addresses.inet.InternetAddress;
import com.netifera.platform.util.locators.TCPSocketLocator;

public class TCPConnectScanner extends AbstractPortscanner {	
	private int errorCount = 0;
	private int errorThreshold = 30;
	private boolean skipUnreachable = true;
	private BitSet badHostSet;
	private AtomicInteger outstandingConnects;
	private Set<TCPConnectServiceDetector> detectors = Collections.synchronizedSet(new HashSet<TCPConnectServiceDetector>());
	private int firstPort;

	@Override
	protected void scannerRun() {
		outstandingConnects = new AtomicInteger(0);

		int hostCount = targetNetwork.size();
		badHostSet = new BitSet(hostCount);
		
		firstPort = targetPorts.contains(80) ? 80 : targetPorts.get(0);
		context.setTitle("TCP connect scan "+targetNetwork);
		context.setStatus("Scanning port "+firstPort);
		context.setTotalWork(targetNetwork.size()*targetPorts.size());
		
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
			context.setStatus("Cancelling "+outstandingConnects.get()+" connections");
			synchronized(detectors) {
				for (TCPConnectServiceDetector detector: detectors.toArray(new TCPConnectServiceDetector[detectors.size()]))
					detector.cancel(false);
			}
			context.warning("Interrupted");
//			Thread.currentThread().interrupt();
//			return;
		} catch (Exception e) {
			context.exception("Exception", e);
		}
		while (outstandingConnects.get() != 0) {
			context.debug("Outstanding connects "+outstandingConnects.get());
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				context.warning("Interrupted");
				synchronized(detectors) {
					for (TCPConnectServiceDetector detector: detectors.toArray(new TCPConnectServiceDetector[detectors.size()]))
						detector.cancel(false);
				}
				Thread.currentThread().interrupt();
				return;
			}
		}
	}

	private boolean scanFirstPort(final int port) throws InterruptedException {
		for (int i = 0; i < targetNetwork.size(); i++) {
			final TCPSocketLocator locator = new TCPSocketLocator(targetNetwork.get(i),port);
			try {
				final int index = i;
				final TCPConnectServiceDetector detector = new TCPConnectServiceDetector(locator, context.getLogger());
				detector.detect(new ITCPConnectServiceDetectorListener() {
					public void badTarget(final TCPSocketLocator locator) {
						markTargetBad(index, new Runnable() {
							public void run() {
								context.worked(targetPorts.size()-1); //XXX is this number ok? might not be the first port we scan
								context.debug("Skipping unreachable host "+locator.getAddress());
							}
						});
					}

					public void connecting(TCPSocketLocator locator) {
						detectors.add(detector);
						outstandingConnects.incrementAndGet();
					}

					public void finished(TCPSocketLocator locator) {
						outstandingConnects.decrementAndGet();
						detectors.remove(detector);
						context.worked(1);
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
				});
				waitDelay();
			} catch (PortUnreachableException e) {
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
		}
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
			
			final TCPSocketLocator locator = new TCPSocketLocator(target, port);

			try {
				final TCPConnectServiceDetector detector = new TCPConnectServiceDetector(locator, context.getLogger());
				detector.detect(new ITCPConnectServiceDetectorListener() {

					public void badTarget(final TCPSocketLocator locator) {
						markTargetBad(index, new Runnable() {
							public void run() {
								context.worked(targetPorts.size()-1); //XXX is this number ok? might not be the first port we scan
//								context.debug("Skipping unreachable host "+locator.getAddress()+", "+e.getMessage());
							}
						});
					}

					public void connecting(TCPSocketLocator locator) {
						detectors.add(detector);
						outstandingConnects.incrementAndGet();
					}

					public void finished(TCPSocketLocator locator) {
						outstandingConnects.decrementAndGet();
						detectors.remove(detector);
						context.worked(1);
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

				});
				waitDelay();
			} catch (PortUnreachableException e) {
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
		}
		return true;
	}

	private void markTargetBad(int index, Runnable runnable) {
		if (!skipUnreachable)
			return;
		synchronized (badHostSet) {
			if (!badHostSet.get(index)) {
				badHostSet.set(index);
				runnable.run();
			}
		}
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
		super.setupToolOptions();
	}
}
