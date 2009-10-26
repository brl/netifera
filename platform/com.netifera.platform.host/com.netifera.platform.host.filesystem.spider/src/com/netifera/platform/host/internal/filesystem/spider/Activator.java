package com.netifera.platform.host.internal.filesystem.spider;

import java.util.ArrayList;
import java.util.List;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import com.netifera.platform.host.filesystem.spider.IFileSystemSpiderModule;

public class Activator implements BundleActivator {

	private ServiceTracker fileSystemSpiderModulesTracker;

	private static Activator instance;
	
	public static Activator getInstance() {
		return instance;
	}
	
	public void start(BundleContext context) throws Exception {
		instance = this;
		fileSystemSpiderModulesTracker = new ServiceTracker(context, IFileSystemSpiderModule.class.getName(), null);
		fileSystemSpiderModulesTracker.open();
	}

	public void stop(BundleContext context) throws Exception {
		// TODO Auto-generated method stub

	}

	public List<IFileSystemSpiderModule> getFileSystemSpiderModules() {
		List<IFileSystemSpiderModule> modules = new ArrayList<IFileSystemSpiderModule>();
		for (Object o: fileSystemSpiderModulesTracker.getServices())
			modules.add((IFileSystemSpiderModule) o);
		return modules;
	}
}
