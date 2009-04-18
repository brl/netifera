package com.netifera.platform.net.internal.daemon.sniffing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.netifera.platform.api.dispatcher.IMessenger;
import com.netifera.platform.api.dispatcher.MessengerException;
import com.netifera.platform.api.log.ILogger;
import com.netifera.platform.net.internal.daemon.remote.InterfaceRecord;
import com.netifera.platform.net.internal.daemon.remote.RequestInterfaceInformation;
import com.netifera.platform.net.internal.daemon.remote.SetInterfaceEnableState;
import com.netifera.platform.net.pcap.ICaptureInterface;
import com.netifera.platform.net.sniffing.ISniffingEngineService;

public class SniffingDaemonInterfaces {
	/*
	 * Network interfaces which have been enabled with the enableInterfaces()
	 * method.
	 */
	private final Set<ICaptureInterface> enabledInterfaces;
	
	private ISniffingEngineService sniffingEngine;
	private ILogger logger;
	private boolean isInitialized;
	
	public void requestInterfaceInformation(IMessenger messenger, RequestInterfaceInformation msg) throws MessengerException {
		verifyInitialized();
		final List<InterfaceRecord> result = new ArrayList<InterfaceRecord>();
		
		for(ICaptureInterface iface : getInterfaces()) 
			result.add(new InterfaceRecord(iface.getName(), iface.toString(), iface.captureAvailable(), isEnabled(iface)));
		messenger.emitMessage(msg.createResponse(result));
	}
	
	public void setInterfaceEnableState(IMessenger messenger, SetInterfaceEnableState msg) throws MessengerException {
		verifyInitialized();
		for(InterfaceRecord iface : msg.getInterfaceRecords()) {
			final ICaptureInterface captureInterface = lookupInterfaceByName(iface.getName());
			if(captureInterface == null) {
				logger.warning("No capture interface found with name : " + iface.getName());
			} else {
				if(iface.isEnabled()) {
					enableInterface(captureInterface);
				} else {
					disableInterface(captureInterface);
				}
			}
		}
		messenger.respondOk(msg);		
	}
	
	public SniffingDaemonInterfaces() {
		enabledInterfaces = new HashSet<ICaptureInterface>();		
	}

	public void setServices(ILogger logger, ISniffingEngineService sniffingEngine) {
		this.logger = logger;
		this.sniffingEngine = sniffingEngine;
		enableAllInterfaces();
		this.isInitialized = true;
	}
	
	private void verifyInitialized() {
		if(!isInitialized) {
			throw new IllegalStateException("Sniffing Daemon Interface subsystem is not initialized");
		}
	}
	
	private void enableAllInterfaces() {
		synchronized(enabledInterfaces) {
			enabledInterfaces.clear();
			for(ICaptureInterface iface : getInterfaces()) 
				if(iface.captureAvailable())
					enabledInterfaces.add(iface);
		}
	}
	private Collection<ICaptureInterface> getInterfaces() {
		return sniffingEngine.getInterfaces();
	}
	
	Collection<ICaptureInterface> getEnabledInterfaces() {
		return Collections.unmodifiableCollection(enabledInterfaces);
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
			//System.out.println("getInterfaces " + getInterfaces());
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
	
	
	private ICaptureInterface lookupInterfaceByName(String name) {
		for(ICaptureInterface iface : getInterfaces()) {
			if(iface.getName().equals(name))
				return iface;
		}
		return null;
	}
	
	
}
