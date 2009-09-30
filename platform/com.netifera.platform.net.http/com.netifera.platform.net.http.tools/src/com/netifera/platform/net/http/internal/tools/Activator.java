package com.netifera.platform.net.http.internal.tools;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import com.netifera.platform.api.model.IModelService;
import com.netifera.platform.api.probe.IProbeManagerService;
import com.netifera.platform.net.dns.model.IDomainEntityFactory;
import com.netifera.platform.net.dns.service.nameresolver.INameResolver;
import com.netifera.platform.net.http.spider.IWebSpiderModule;
import com.netifera.platform.net.http.web.model.IWebEntityFactory;
import com.netifera.platform.net.model.INetworkEntityFactory;
import com.netifera.platform.net.sockets.ISocketEngineService;

public class Activator implements BundleActivator {

	private ServiceTracker modelTracker;
	private ServiceTracker probeManagerTracker;
	private ServiceTracker socketEngineTracker;
	private ServiceTracker networkEntityFactoryTracker;
	private ServiceTracker domainEntityFactoryTracker;
	private ServiceTracker webEntityFactoryTracker;
	private ServiceTracker nameResolverTracker;
	private ServiceTracker webSpiderModulesTracker;

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
		
		socketEngineTracker = new ServiceTracker(context, ISocketEngineService.class.getName(), null);
		socketEngineTracker.open();
		
		networkEntityFactoryTracker = new ServiceTracker(context, INetworkEntityFactory.class.getName(), null);
		networkEntityFactoryTracker.open();

		domainEntityFactoryTracker = new ServiceTracker(context, IDomainEntityFactory.class.getName(), null);
		domainEntityFactoryTracker.open();

		webEntityFactoryTracker = new ServiceTracker(context, IWebEntityFactory.class.getName(), null);
		webEntityFactoryTracker.open();

		nameResolverTracker = new ServiceTracker(context, INameResolver.class.getName(), null);
		nameResolverTracker.open();

		webSpiderModulesTracker = new ServiceTracker(context, IWebSpiderModule.class.getName(), null);
		webSpiderModulesTracker.open();
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
	
	public ISocketEngineService getSocketEngine() {
		return (ISocketEngineService) socketEngineTracker.getService();
	}
	
	public INetworkEntityFactory getNetworkEntityFactory() {
		return (INetworkEntityFactory) networkEntityFactoryTracker.getService();
	}

	public IDomainEntityFactory getDomainEntityFactory() {
		return (IDomainEntityFactory) domainEntityFactoryTracker.getService();
	}

	public IWebEntityFactory getWebEntityFactory() {
		return (IWebEntityFactory) webEntityFactoryTracker.getService();
	}

	public INameResolver getNameResolver() {
		return (INameResolver) nameResolverTracker.getService();
	}

	public IWebSpiderModule[] getWebSpiderModules() {
		return (IWebSpiderModule[]) webSpiderModulesTracker.getServices();
	}
}
