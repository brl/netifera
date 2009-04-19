package com.netifera.platform.net.wifi.daemon;

import com.netifera.platform.net.daemon.sniffing.module.IPacketModuleContext;
import com.netifera.platform.net.daemon.sniffing.module.ISniffingModule;
import com.netifera.platform.net.wifi.packets.WiFiFrame;

public interface IWirelessSniffingModule extends ISniffingModule {
	void handleWifiFrame(WiFiFrame wifi, IPacketModuleContext ctx);
}
