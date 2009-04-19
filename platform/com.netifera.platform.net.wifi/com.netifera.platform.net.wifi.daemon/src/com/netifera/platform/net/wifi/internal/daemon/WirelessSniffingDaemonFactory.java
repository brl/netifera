package com.netifera.platform.net.wifi.internal.daemon;

import com.netifera.platform.api.events.IEventHandler;
import com.netifera.platform.api.probe.IProbe;
import com.netifera.platform.net.daemon.sniffing.extend.AbstractSniffingDaemonFactory;
import com.netifera.platform.net.daemon.sniffing.extend.AbstractSniffingDaemonStub;
import com.netifera.platform.net.wifi.daemon.IWifiSniffingDaemon;
import com.netifera.platform.net.wifi.daemon.IWifiSniffingDaemonFactory;

public class WirelessSniffingDaemonFactory extends AbstractSniffingDaemonFactory implements IWifiSniffingDaemonFactory {

	public WirelessSniffingDaemonFactory() {
		super(WirelessSniffingDaemon.MESSAGE_PREFIX);
	}

	public IWifiSniffingDaemon createWifiForProbe(IProbe probe, IEventHandler changeHandler) {
		return (IWifiSniffingDaemon) createForProbe(probe, changeHandler);		
	}
	
	public IWifiSniffingDaemon lookupWifiForProbe(IProbe probe) {
		return (IWifiSniffingDaemon) lookupForProbe(probe);
	}
	
	@Override
	protected AbstractSniffingDaemonStub createStub(IProbe probe,
			IEventHandler changeHandler) {
		return new WirelessSniffingDaemonStub(probe, getLogger(), changeHandler);
	}


	

}
