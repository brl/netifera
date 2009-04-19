package com.netifera.platform.net.wifi.internal.daemon;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.netifera.platform.api.events.IEventHandler;
import com.netifera.platform.api.log.ILogger;
import com.netifera.platform.api.probe.IProbe;
import com.netifera.platform.net.daemon.sniffing.extend.AbstractSniffingDaemonStub;
import com.netifera.platform.net.daemon.sniffing.module.ISniffingModule;
import com.netifera.platform.net.wifi.daemon.IWifiSniffingDaemon;
import com.netifera.platform.net.wifi.internal.daemon.remote.ModuleRecord;
import com.netifera.platform.net.wifi.internal.daemon.remote.RequestWirelessModuleInformation;
import com.netifera.platform.net.wifi.internal.daemon.remote.SetWirelessModuleEnableState;

public class WirelessSniffingDaemonStub extends AbstractSniffingDaemonStub implements IWifiSniffingDaemon {

	private List<ModuleRecord> moduleRecords;
	public WirelessSniffingDaemonStub(IProbe probe,
			ILogger logger, IEventHandler changeHandler) {
		super(WirelessSniffingDaemon.MESSAGE_PREFIX, probe, logger, changeHandler);
		refreshWirelessModuleInformation();
	}


	public Set<ISniffingModule> getWirelessModules() {
		synchronized(lock) {
			while(moduleRecords == null) {
				try {
					lock.wait();
				} catch(InterruptedException e) {
					Thread.currentThread().interrupt();
					return Collections.emptySet();
				}
			}
		}
		return new HashSet<ISniffingModule>(moduleRecords);
	}

	

	public boolean isWirelessModuleEnabled(ISniffingModule module) {
		synchronized(lock) {
			while(moduleRecords == null) {
				try {
					lock.wait();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return false;
				}
			}
			
			for(ModuleRecord record : moduleRecords) {
				if(record.getName().equals(module.getName())) {
					return record.isEnabled();
				}
			}
		}
		getLogger().warning("Wireless module not found for name " + module.getName());
		return false;
	}
	public void setWirelessEnabled(ISniffingModule module, boolean enable) {
		final ModuleRecord moduleRecord = new ModuleRecord(module.getName(), enable);
		enqueue(new SetWirelessModuleEnableState(moduleRecord));
		
	}
	
	
	private List<ModuleRecord> getWirelessModuleRecords() {
		final RequestWirelessModuleInformation response = (RequestWirelessModuleInformation) exchangeMessage(new RequestWirelessModuleInformation());
		if(response == null) {
			getLogger().warning("Failed to get module information: " + getLastError());
			return null;
		}
		return response.getModuleRecords();
	}
	
	private void refreshWirelessModuleInformation() {
		Thread t = new Thread(new Runnable() {

			public void run() {
				waitForEmptySendQueue();
				synchronized(lock) {
					moduleRecords = getWirelessModuleRecords();
					lock.notifyAll();
				}
				
			}
			
		});
		t.start();
	}

}
