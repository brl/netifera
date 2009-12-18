package com.netifera.platform.net.tools.portscanning;


import java.util.Random;

import com.netifera.platform.api.iterables.IndexedIterable;
import com.netifera.platform.api.tools.ITool;
import com.netifera.platform.api.tools.IToolContext;
import com.netifera.platform.api.tools.ToolException;
import com.netifera.platform.tools.RequiredOptionMissingException;
import com.netifera.platform.util.PortSet;
import com.netifera.platform.util.addresses.inet.InternetAddress;

public abstract class AbstractPortscanner implements ITool {
	protected IToolContext context;
	protected IndexedIterable<InternetAddress> targetNetwork;
	protected PortSet targetPorts;
	private int delay = 0;
	private Random random = new Random(System.currentTimeMillis());

	public void run(IToolContext context) throws ToolException {
		assert(context != null);
		this.context = context;

		setupPortscannerOptions();
		//task.setTotalWork(targetNetwork.itemCount() * targetPorts.itemCount());
		try {
			scannerRun();
		} finally {
			context.done();
		}
	}
	
	protected abstract void scannerRun() throws ToolException;
	
	protected void setupToolOptions() throws ToolException {
		/* Override me for handling tool specific options */
	}
	
	@SuppressWarnings("unchecked")
	private void setupPortscannerOptions() throws ToolException {
		setupToolOptions();

		targetNetwork = (IndexedIterable<InternetAddress>) context.getConfiguration().get("target");
		if(targetNetwork == null) {
			throw new RequiredOptionMissingException("target");
		}

		String portsString = (String)context.getConfiguration().get("ports");
		if(portsString == null) {
			throw new RequiredOptionMissingException("ports");
		}
		try {
			targetPorts = new PortSet(portsString);
		} catch (IllegalArgumentException e) {
			throw new ToolException("Invalid ports: "+portsString);
		}
		
		Integer delay = (Integer)context.getConfiguration().get("delay");
		if(delay != null) {
			this.delay = delay;
		}
	}

	protected void waitDelay() throws InterruptedException {
		if (delay > 0) {
			int randomDelay = random.nextInt(delay/2) + delay;
			Thread.sleep(randomDelay);
		}
	}
}