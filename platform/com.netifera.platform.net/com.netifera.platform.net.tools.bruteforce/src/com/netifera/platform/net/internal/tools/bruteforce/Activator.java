package com.netifera.platform.net.internal.tools.bruteforce;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import com.netifera.platform.api.model.IModelService;
import com.netifera.platform.api.probe.IProbeManagerService;
import com.netifera.platform.net.model.INetworkEntityFactory;
import com.netifera.platform.net.wordlists.IWordListManager;

public class Activator implements BundleActivator {

	private ServiceTracker modelTracker;
	private ServiceTracker probeManagerTracker;
	private ServiceTracker networkEntityFactoryTracker;
	private ServiceTracker wordlistsTracker;
	
	private static Activator instance;
	
	public static Activator getInstance() {
		return instance;
	}
	
	public void start(BundleContext context) throws Exception {
		instance = this;
		modelTracker = new ServiceTracker(context, IModelService.class.getName(), null);
		modelTracker.open();
		
		probeManagerTracker = new ServiceTracker(context, IProbeManagerService.class.getName(), null);
		probeManagerTracker.open();
		
		networkEntityFactoryTracker = new ServiceTracker(context, INetworkEntityFactory.class.getName(), null);
		networkEntityFactoryTracker.open();

		wordlistsTracker = new ServiceTracker(context, IWordListManager.class.getName(), null);
		wordlistsTracker.open();
	}

	public void stop(BundleContext context) throws Exception {
		// TODO Auto-generated method stub
		
	}

	public IModelService getModelService() {
		return (IModelService) modelTracker.getService();
	}
	
	public IProbeManagerService getProbeManager() {
		return (IProbeManagerService) probeManagerTracker.getService();
	}
	
	public INetworkEntityFactory getNetworkEntityFactory() {
		return (INetworkEntityFactory) networkEntityFactoryTracker.getService();
	}
	
	public IWordListManager getWordListManager() {
		return (IWordListManager) wordlistsTracker.getService();
	}
}
