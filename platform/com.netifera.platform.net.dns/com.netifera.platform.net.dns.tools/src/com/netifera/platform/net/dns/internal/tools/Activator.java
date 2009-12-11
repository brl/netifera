package com.netifera.platform.net.dns.internal.tools;

import org.jboss.netty.channel.socket.DatagramChannelFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import com.netifera.platform.api.model.IModelService;
import com.netifera.platform.api.probe.IProbeManagerService;
import com.netifera.platform.net.dns.model.IDomainEntityFactory;
import com.netifera.platform.net.dns.service.nameresolver.INameResolver;
import com.netifera.platform.net.model.INetworkEntityFactory;

public class Activator implements BundleActivator {

	private ServiceTracker modelTracker;
	private ServiceTracker probeManagerTracker;
	private ServiceTracker datagramChannelFactoryTracker;
	private ServiceTracker networkEntityFactoryTracker;
	private ServiceTracker domainEntityFactoryTracker;
	private ServiceTracker nameResolverTracker;
	
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
		
		datagramChannelFactoryTracker = new ServiceTracker(context, DatagramChannelFactory.class.getName(), null);
		datagramChannelFactoryTracker.open();
		
		networkEntityFactoryTracker = new ServiceTracker(context, INetworkEntityFactory.class.getName(), null);
		networkEntityFactoryTracker.open();
		
		domainEntityFactoryTracker = new ServiceTracker(context, IDomainEntityFactory.class.getName(), null);
		domainEntityFactoryTracker.open();

		nameResolverTracker = new ServiceTracker(context, INameResolver.class.getName(), null);
		nameResolverTracker.open();
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
	
	public DatagramChannelFactory getDatagramChannelFactory() {
		return (DatagramChannelFactory) datagramChannelFactoryTracker.getService();
	}
	
	public INetworkEntityFactory getNetworkEntityFactory() {
		return (INetworkEntityFactory) networkEntityFactoryTracker.getService();
	}

	public IDomainEntityFactory getDomainEntityFactory() {
		return (IDomainEntityFactory) domainEntityFactoryTracker.getService();
	}
	
	public INameResolver getNameResolver() {
		return (INameResolver) nameResolverTracker.getService();
	}
}
