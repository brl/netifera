package com.netifera.platform.net.internal.daemon.sniffing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.netifera.platform.api.dispatcher.IMessenger;
import com.netifera.platform.api.dispatcher.MessengerException;
import com.netifera.platform.net.daemon.sniffing.extend.AbstractSniffingDaemon;
import com.netifera.platform.net.internal.daemon.remote.InterfaceRecord;
import com.netifera.platform.net.internal.daemon.remote.RequestInterfaceInformation;
import com.netifera.platform.net.internal.daemon.remote.SetInterfaceEnableState;
import com.netifera.platform.net.pcap.ICaptureInterface;

public class SniffingDaemonInterfaces {
	
	private final Set<ICaptureInterface> enabledInterfaces;
	
	private final AbstractSniffingDaemon sniffingDaemon;
		
	public void requestInterfaceInformation(IMessenger messenger, RequestInterfaceInformation msg) throws MessengerException {
		final List<InterfaceRecord> result = new ArrayList<InterfaceRecord>();
		
		for(ICaptureInterface iface : getInterfaces()) 
			result.add(new InterfaceRecord(iface.getName(), iface.toString(), iface.captureAvailable(), isEnabled(iface)));
		messenger.emitMessage(msg.createResponse(sniffingDaemon.getMessagePrefix(), result));
	}
	
	public void setInterfaceEnableState(IMessenger messenger, SetInterfaceEnableState msg) throws MessengerException {
		for(InterfaceRecord iface : msg.getInterfaceRecords()) {
			final ICaptureInterface captureInterface = lookupInterfaceByName(iface.getName());
			if(captureInterface == null) {
				sniffingDaemon.getLogger().warning("No capture interface found with name : " + iface.getName());
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
	
	public SniffingDaemonInterfaces(AbstractSniffingDaemon sniffingDaemon) {
		enabledInterfaces = new HashSet<ICaptureInterface>();
		this.sniffingDaemon = sniffingDaemon;
	}

	public void initialize() {
		enableAllInterfaces();
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
		return sniffingDaemon.getSniffingEngine().getInterfaces();
	}
	
	public Collection<ICaptureInterface> getEnabledInterfaces() {
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
	
	
	private ICaptureInterface lookupInterfaceByName(String name) {
		for(ICaptureInterface iface : getInterfaces()) {
			if(iface.getName().equals(name))
				return iface;
		}
		return null;
	}
	
	
}
