package com.netifera.platform.net.cifs.internal.tools;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import com.netifera.platform.net.model.INetworkEntityFactory;
import com.netifera.platform.net.sockets.ISocketEngineService;
import com.netifera.platform.net.wordlists.IWordList;

public class Activator implements BundleActivator {

	private ServiceTracker socketEngineTracker;
	private ServiceTracker networkEntityFactoryTracker;
	private ServiceTracker wordlistsTracker;
	
	private static Activator instance;
	
	public static Activator getInstance() {
		return instance;
	}
	
	public void start(BundleContext context) throws Exception {
		instance = this;
		
		socketEngineTracker = new ServiceTracker(context, ISocketEngineService.class.getName(), null);
		socketEngineTracker.open();
		
		networkEntityFactoryTracker = new ServiceTracker(context, INetworkEntityFactory.class.getName(), null);
		networkEntityFactoryTracker.open();

		wordlistsTracker = new ServiceTracker(context, IWordList.class.getName(), null);
		wordlistsTracker.open();
	}

	public void stop(BundleContext context) throws Exception {
		// TODO Auto-generated method stub
		
	}

	public ISocketEngineService getSocketEngine() {
		return (ISocketEngineService) socketEngineTracker.getService();
	}
	
	public INetworkEntityFactory getNetworkEntityFactory() {
		return (INetworkEntityFactory) networkEntityFactoryTracker.getService();
	}
	
	public IWordList getWordList(String name) {
		for (Object wordlist: wordlistsTracker.getServices()) {
			if (name.equals(((IWordList)wordlist).getName()))
					return (IWordList) wordlist;
		}
		return null;
	}
}
