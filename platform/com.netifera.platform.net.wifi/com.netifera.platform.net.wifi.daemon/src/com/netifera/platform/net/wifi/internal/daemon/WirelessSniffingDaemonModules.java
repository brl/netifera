package com.netifera.platform.net.wifi.internal.daemon;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.netifera.platform.api.dispatcher.IMessenger;
import com.netifera.platform.api.dispatcher.MessengerException;
import com.netifera.platform.net.daemon.sniffing.extend.IModuleExecutionProvider;
import com.netifera.platform.net.daemon.sniffing.model.NetworkInterfaceEntity;
import com.netifera.platform.net.daemon.sniffing.module.ISniffingModule;
import com.netifera.platform.net.pcap.ICaptureInterface;
import com.netifera.platform.net.wifi.daemon.IWirelessSniffingModule;
import com.netifera.platform.net.wifi.internal.daemon.remote.ModuleRecord;
import com.netifera.platform.net.wifi.internal.daemon.remote.RequestWirelessModuleInformation;
import com.netifera.platform.net.wifi.internal.daemon.remote.SetWirelessModuleEnableState;
import com.netifera.platform.net.wifi.pcap.IWirelessCaptureInterface;
import com.netifera.platform.net.wifi.sniffing.IWifiSniffingEngine;

public class WirelessSniffingDaemonModules implements IModuleExecutionProvider {
	
	private final Set<IWirelessSniffingModule> wirelessModules;
	private final Set<EnabledWifiModule> enabledModules;
	private final Map<String, IWirelessSniffingModule> modulesByName;
	
	private final WirelessSniffingDaemon daemon;
	
	
	private boolean running;
	
	public WirelessSniffingDaemonModules(WirelessSniffingDaemon daemon) {
		wirelessModules = new HashSet<IWirelessSniffingModule>();
		enabledModules = new HashSet<EnabledWifiModule>();
		modulesByName = new HashMap<String, IWirelessSniffingModule>();
		this.daemon = daemon;
	}
	
	void requestModuleInformation(IMessenger messenger, RequestWirelessModuleInformation msg) throws MessengerException {
		final List<ModuleRecord> modules = new ArrayList<ModuleRecord>();
		for(IWirelessSniffingModule m : wirelessModules) {
			modules.add(new ModuleRecord(m.getName(), isWirelessModuleEnabled(m)));
		}
		messenger.emitMessage(msg.createResponse(modules));
		
	}
	
	void setWirelessModuleEnableState(IMessenger messenger, SetWirelessModuleEnableState msg) throws MessengerException {
		for(ModuleRecord record : msg.getModuleRecords()) {
			final IWirelessSniffingModule sniffingModule = modulesByName.get(record.getName());
			if(sniffingModule == null) {
				final String error = "No wireless sniffing module found with name " + record.getName();
				daemon.getLogger().warning(error);
				messenger.respondError(msg, error);
				return;
			}
			
			setModuleEnabled(sniffingModule, record.isEnabled());
			messenger.respondOk(msg);
		}
	}
	
	
	private boolean isWirelessModuleEnabled(ISniffingModule module) {
		return findEnabledModule(module) != null;
	}
	
	private EnabledWifiModule findEnabledModule(ISniffingModule module) {
		for(EnabledWifiModule m : enabledModules)
			if(m.getModule().equals(module))
				return m;
		return null;
	}
	
	private void setModuleEnabled(IWirelessSniffingModule module, boolean isEnabled) {
		if(isEnabled)
			enableModule(module);
		else
			disableModule(module);
	}
	
	private void enableModule(IWirelessSniffingModule module) {
		checkValidStateChange(module);
		synchronized(enabledModules) {
			if(findEnabledModule(module) != null) 
				return;
			enabledModules.add(new EnabledWifiModule(module, daemon.getLogger()));
		}
		
	}
	
	private void disableModule(IWirelessSniffingModule module) {
		checkValidStateChange(module);
		synchronized (enabledModules) {
			final EnabledWifiModule enabledModule = findEnabledModule(module);
			if(enabledModule != null)
				enabledModules.remove(enabledModule);
		}
		
	}
	
	private void checkValidStateChange(IWirelessSniffingModule module) {
		if(running)
			throw new IllegalStateException("Cannot enable/disable modules while daemon is running");
		if(!wirelessModules.contains(module))
			throw new IllegalArgumentException("Cannot enable/disable unknown module : " + module.getName());
	}
	
	public void startModules(Collection<ICaptureInterface> enabledInterfaces, 
			long spaceId, long realmId, boolean createInterfaceEntities) {
		if(running)
			return;
		
		
		final Set<WifiDaemonInterface> ifaces = new HashSet<WifiDaemonInterface>();
		for(ICaptureInterface iface : enabledInterfaces) {
			if(!(iface instanceof IWirelessCaptureInterface)) {
				// XXX
			}
			final IWirelessCaptureInterface wifiInterface = (IWirelessCaptureInterface) iface;
			if(createInterfaceEntities) {
				final NetworkInterfaceEntity interfaceEntity = daemon.createNetworkInterfaceEntity(realmId, spaceId, iface.getName());
				ifaces.add(new WifiDaemonInterface(wifiInterface, interfaceEntity.getId()));
			} else {
				ifaces.add(new WifiDaemonInterface(wifiInterface, realmId));
			}
			
		}
		
		for(EnabledWifiModule module : enabledModules) {
			module.start((IWifiSniffingEngine) daemon.getSniffingEngine(), ifaces, spaceId);
		}
		running = true;
	}
	
	public void stopModules() {
		if(!running)
			return;
		for(EnabledWifiModule module : enabledModules) 
			module.stop();
		running = false;
	}
	
	void addModule(IWirelessSniffingModule module) {
		synchronized (wirelessModules) {
			wirelessModules.add(module);
			modulesByName.put(module.getName(), module);
		}
		enableModule(module);
	}
	
	void removeModule(IWirelessSniffingModule module) {
		disableModule(module);
		synchronized(wirelessModules) {
			wirelessModules.remove(module);
			modulesByName.remove(module.getName());
		}
	}

	

	
}
