package com.netifera.platform.net.daemon.sniffing.extend;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.netifera.platform.net.pcap.ICaptureInterface;
import com.netifera.platform.net.sniffing.ISniffingEngineService;

public class SniffingDaemonInterfaces {
	/*
	 * Network interfaces which have been enabled with the enableInterfaces()
	 * method.
	 */
	private final Set<ICaptureInterface> enabledInterfaces;
	
	private ISniffingEngineService sniffingEngine;
	
	
	public SniffingDaemonInterfaces() {
		enabledInterfaces = new HashSet<ICaptureInterface>();
	}
	
	
	void setSniffingEngine(ISniffingEngineService sniffingEngine) {
		this.sniffingEngine = sniffingEngine;
	}
	
	Collection<ICaptureInterface> getInterfaces() {
		if(sniffingEngine == null) {
			throw new IllegalStateException("Cannot call getInterfaces() because no sniffing engine has been set");
		}
		return sniffingEngine.getInterfaces();
	}
	
	Collection<ICaptureInterface> getEnabledInterfaces() {
		return enabledInterfaces;
	}
	
	boolean hasEnabledInterfaces() {
		return !enabledInterfaces.isEmpty();
	}
	
	boolean isEnabled(ICaptureInterface iface) {
		return enabledInterfaces.contains(iface);
	}
	
	void disableAllInterfaces() {
		synchronized (enabledInterfaces) {
			enabledInterfaces.clear();
		}
	}
	
	void enableInterface(ICaptureInterface iface) {
		if(!getInterfaces().contains(iface)) {
			System.out.println("getInterfaces " + getInterfaces());
			throw new IllegalArgumentException("Unknown interface passed to enableInterface() : " + iface);
		}
		if(!iface.captureAvailable()) {
			throw new IllegalArgumentException("Cannot enable unavailable interface : " + iface);
		}
		
		synchronized (enabledInterfaces) {
			enabledInterfaces.add(iface);
		}
	}
	
	void disableInterface(ICaptureInterface iface) {
		if(!getInterfaces().contains(iface)) {
			throw new IllegalArgumentException("Unknown interface passed to enableInterface() : " + iface);
		}
		
		synchronized (enabledInterfaces) {
			enabledInterfaces.remove(iface);
		}
	}
	
	
	ICaptureInterface lookupInterfaceByName(String name) {
		for(ICaptureInterface iface : getInterfaces()) {
			if(iface.getName().equals(name))
				return iface;
		}
		return null;
	}
	
	
}
