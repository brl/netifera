package com.netifera.platform.host.filesystem.tools.internal;

import java.util.ArrayList;
import java.util.List;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import com.netifera.platform.api.model.IModelService;
import com.netifera.platform.api.probe.IProbeManagerService;
import com.netifera.platform.host.filesystem.spider.IFileSystemSpiderModule;
import com.netifera.platform.services.IServiceFactory;

public class Activator implements BundleActivator {

	private ServiceTracker modelTracker;
	private ServiceTracker probeManagerTracker;
	private ServiceTracker serviceFactoryTracker;
	private ServiceTracker fileSystemSpiderModulesTracker;

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
		
		serviceFactoryTracker = new ServiceTracker(context, IServiceFactory.class.getName(), null);
		serviceFactoryTracker.open();

		fileSystemSpiderModulesTracker = new ServiceTracker(context, IFileSystemSpiderModule.class.getName(), null);
		fileSystemSpiderModulesTracker.open();
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
	
	public IServiceFactory getServiceFactory() {
		return (IServiceFactory) serviceFactoryTracker.getService();
	}
	
	public List<IFileSystemSpiderModule> getFileSystemSpiderModules() {
		List<IFileSystemSpiderModule> modules = new ArrayList<IFileSystemSpiderModule>();
		for (Object o: fileSystemSpiderModulesTracker.getServices())
			modules.add((IFileSystemSpiderModule) o);
		return modules;
	}
}
