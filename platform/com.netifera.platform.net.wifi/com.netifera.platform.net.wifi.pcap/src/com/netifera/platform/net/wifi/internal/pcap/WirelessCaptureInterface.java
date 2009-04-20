package com.netifera.platform.net.wifi.internal.pcap;

import com.netifera.platform.net.pcap.ICaptureInterface;
import com.netifera.platform.net.pcap.IPacketCapture;
import com.netifera.platform.net.pcap.IPacketHandler;
import com.netifera.platform.net.wifi.pcap.IWifiPacketCapture;
import com.netifera.platform.net.wifi.pcap.IWifiPacketCaptureFactory;
import com.netifera.platform.net.wifi.pcap.IWirelessCaptureInterface;

public class WirelessCaptureInterface implements IWirelessCaptureInterface {

	final private IWifiPacketCaptureFactory wifiFactory;
	final private String interfaceName;
	final private boolean isAvailable;
	public WirelessCaptureInterface(IWifiPacketCaptureFactory wifiPcapFactory, String interfaceName, boolean isAvailable) {
		this.wifiFactory = wifiPcapFactory;
		this.interfaceName = interfaceName;
		this.isAvailable = isAvailable;
	}
	
	public boolean isMonitorModeCapable() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean captureAvailable() {
		return isAvailable;
	}

	public String getName() {
		return interfaceName;
	}
	
	
	
	
	public String toString() {
		return "Wireless: " + interfaceName;
	}
	
	public boolean equals(Object other) {
		if(!(other instanceof ICaptureInterface)) {
			return false;
		}
		return ((ICaptureInterface)other).getName().equals(getName());
	}
	
	public int hashCode() {
		return getName().hashCode();
	}
	public IPacketCapture pcapCreate(int snaplen, boolean promiscuous,
			int timeout, IPacketHandler handler) {
		return wifiFactory.create(this, snaplen, promiscuous, timeout, handler);
	}

}
