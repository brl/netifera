package com.netifera.platform.net.wifi.daemon;

import com.netifera.platform.api.events.IEventHandler;
import com.netifera.platform.api.probe.IProbe;

public interface IWifiSniffingDaemonFactory  {
	IWifiSniffingDaemon createWifiForProbe(IProbe probe, IEventHandler changeHandler);
	IWifiSniffingDaemon lookupWifiForProbe(IProbe probe);
}
