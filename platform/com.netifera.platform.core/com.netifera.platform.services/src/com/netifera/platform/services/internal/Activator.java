package com.netifera.platform.services.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import com.netifera.platform.services.IServiceFactory;

public class Activator implements BundleActivator {

	private ServiceTracker serviceFactoryTracker;

	private static Activator instance;
	
	public static Activator getInstance() {
		return instance;
	}
	
	public void start(BundleContext context) throws Exception {
		instance = this;
				
		serviceFactoryTracker = new ServiceTracker(context, IServiceFactory.class.getName(), null);
		serviceFactoryTracker.open();
	}

	public void stop(BundleContext context) throws Exception {
		// TODO Auto-generated method stub
		
	}
	
	public IServiceFactory getServiceFactory() {
		return (IServiceFactory) serviceFactoryTracker.getService();
	}
}
