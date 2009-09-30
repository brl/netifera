package com.netifera.platform.net.http.internal.spider;

import java.util.ArrayList;
import java.util.List;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import com.netifera.platform.net.http.spider.IWebSpiderModule;

public class Activator implements BundleActivator {

	private ServiceTracker webSpiderModulesTracker;

	private static Activator instance;
	
	public static Activator getInstance() {
		return instance;
	}
	
	public void start(BundleContext context) throws Exception {
		instance = this;
		webSpiderModulesTracker = new ServiceTracker(context, IWebSpiderModule.class.getName(), null);
		webSpiderModulesTracker.open();
	}

	public void stop(BundleContext context) throws Exception {
		// TODO Auto-generated method stub

	}

	public List<IWebSpiderModule> getWebSpiderModules() {
		List<IWebSpiderModule> modules = new ArrayList<IWebSpiderModule>();
		for (Object o: webSpiderModulesTracker.getServices())
			modules.add((IWebSpiderModule) o);
		return modules;
	}

}
