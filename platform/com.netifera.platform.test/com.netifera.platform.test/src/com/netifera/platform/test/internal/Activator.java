package com.netifera.platform.test.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import com.netifera.platform.api.model.IModelService;
import com.netifera.platform.net.model.INetworkEntityFactory;


public class Activator implements BundleActivator {

	private ServiceTracker modelTracker;
	private ServiceTracker networkEntityFactoryTracker;
	
	private static Activator instance;
	
	public static Activator getInstance() {
		return instance;
	}
	
	public void start(BundleContext context) throws Exception {
		instance = this;
		modelTracker = new ServiceTracker(context, IModelService.class.getName(), null);
		modelTracker.open();
		
		networkEntityFactoryTracker = new ServiceTracker(context, INetworkEntityFactory.class.getName(), null);
		networkEntityFactoryTracker.open();
	}

	public void stop(BundleContext context) throws Exception {
		// TODO Auto-generated method stub
	}

	public IModelService getModelService() {
		return (IModelService) modelTracker.getService();
	}
	
	public INetworkEntityFactory getNetworkEntityFactory() {
		return (INetworkEntityFactory) networkEntityFactoryTracker.getService();
	}
}
