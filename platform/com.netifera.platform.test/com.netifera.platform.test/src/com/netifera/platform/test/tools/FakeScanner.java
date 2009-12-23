package com.netifera.platform.test.tools;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.netifera.platform.api.tools.ToolException;
import com.netifera.platform.net.tools.portscanning.AbstractPortscanner;
import com.netifera.platform.test.internal.Activator;
import com.netifera.platform.util.addresses.inet.InternetAddress;
import com.netifera.platform.util.addresses.inet.TCPSocketAddress;

public class FakeScanner extends AbstractPortscanner {
	private Random random;
	
	@Override
	protected void setupToolOptions() throws ToolException {
		context.setTitle("Fake scan");
		super.setupToolOptions();
	}
	
	@Override
	protected void scannerRun() throws ToolException {
		context.setTitle("Fake scan "+targetNetwork);
		context.setTotalWork(targetNetwork.size()*targetPorts.size());

		random = new Random(System.currentTimeMillis());
		
		try {
			scanAllAddresses();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			context.warning("Interrupted");
			return;
		}
	}

	private void scanAllAddresses() throws InterruptedException {
		for (Integer port: targetPorts) {
			for (InternetAddress address: targetNetwork) {
				if (Thread.currentThread().isInterrupted())
					throw new InterruptedException();
				try {
					randomlyAddService(address, port);
					waitDelay();
					if (Thread.currentThread().isInterrupted())
						return;
				} finally {
					context.worked(1);
				}
			}
		}
	}

	private void randomlyAddService(InternetAddress address, Integer port) {
		if (random.nextFloat() < 0.9)
			return;
		
		TCPSocketAddress peer = new TCPSocketAddress(address, port);
		Map<String,String> serviceInfo = randomlyDetectTCPService();
		if (serviceInfo != null) {
			Activator.getInstance().getNetworkEntityFactory().createService(context.getRealm(), context.getSpaceId(), peer, serviceInfo.get("serviceType"), serviceInfo);
			context.info(serviceInfo.get("serviceType")+" @ "+peer);
		} else {
			Activator.getInstance().getNetworkEntityFactory().createService(context.getRealm(), context.getSpaceId(), peer, null, null);
			context.warning("Unknown service @ " + peer);
		}
	}
	
	private Map<String,String> randomlyDetectTCPService() {
		if (random.nextBoolean()) {
			Map<String,String> serviceInfo = new HashMap<String,String>();
			serviceInfo.put("serviceType", "HTTP");
			return serviceInfo;
		} else {
			return null;
		}
	}
}
