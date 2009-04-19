package com.netifera.platform.net.wifi.internal.daemon;

import com.netifera.platform.api.dispatcher.IMessenger;
import com.netifera.platform.api.dispatcher.MessengerException;
import com.netifera.platform.net.wifi.internal.daemon.remote.RequestWirelessModuleInformation;
import com.netifera.platform.net.wifi.internal.daemon.remote.SetWirelessModuleEnableState;

public interface IWirelessSniffingDaemonMessageHandler {
	void requestWirelessModuleInformation(IMessenger messenger, RequestWirelessModuleInformation message) throws MessengerException;
	void setWirelessModuleEnableState(IMessenger messenger, SetWirelessModuleEnableState message) throws MessengerException;
}
