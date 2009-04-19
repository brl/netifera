	package com.netifera.platform.net.wifi.daemon;

import java.util.Set;

import com.netifera.platform.net.daemon.sniffing.ISniffingDaemon;
import com.netifera.platform.net.daemon.sniffing.module.ISniffingModule;

public interface IWifiSniffingDaemon extends ISniffingDaemon {
	
	void setWirelessEnabled(ISniffingModule module, boolean enable);
	boolean isWirelessModuleEnabled(ISniffingModule module);

	Set<ISniffingModule> getWirelessModules();


}
