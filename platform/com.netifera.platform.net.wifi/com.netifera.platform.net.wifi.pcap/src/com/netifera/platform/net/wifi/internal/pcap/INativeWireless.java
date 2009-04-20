package com.netifera.platform.net.wifi.internal.pcap;

import java.util.Collection;

import com.netifera.platform.net.wifi.pcap.IWifiPacketCapture;
import com.netifera.platform.net.wifi.pcap.IWirelessCaptureInterface;

public interface INativeWireless {
	Collection<IWirelessCaptureInterface> listInterfaces();
	boolean enableMonitorMode(IWifiPacketCapture pcap);
	boolean disableMonitorMode(IWifiPacketCapture pcap);
	int getChannel(IWifiPacketCapture pcap);
	boolean setChannel(IWifiPacketCapture pcap, int channel);

}
