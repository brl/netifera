package com.netifera.platform.net.wifi.internal.pcap;

import java.util.Collection;

import org.osgi.service.component.ComponentContext;

import com.netifera.platform.api.log.ILogManager;
import com.netifera.platform.api.log.ILogger;
import com.netifera.platform.api.system.ISystemService;
import com.netifera.platform.net.pcap.IPacketCapture;
import com.netifera.platform.net.pcap.IPacketCaptureFactoryService;
import com.netifera.platform.net.pcap.IPacketHandler;
import com.netifera.platform.net.wifi.internal.pcap.linux.LinuxNativeWireless;
import com.netifera.platform.net.wifi.internal.pcap.osx.OsxNativeWireless;
import com.netifera.platform.net.wifi.pcap.IWifiPacketCapture;
import com.netifera.platform.net.wifi.pcap.IWifiPacketCaptureFactory;
import com.netifera.platform.net.wifi.pcap.IWirelessCaptureInterface;

public class WifiPacketCaptureFactory implements IWifiPacketCaptureFactory {

	private ISystemService system;
	private IPacketCaptureFactoryService pcapFactory;
	private INativeWireless nativeWireless;
	
	public IWifiPacketCapture create(IWirelessCaptureInterface iface, int snaplen, boolean promiscuous,
			int timeout, IPacketHandler packetHandler) {
		IPacketCapture pcap = pcapFactory.create(iface, snaplen, promiscuous, timeout, packetHandler);
		return new WifiPacketCapture(pcap, nativeWireless);
	}

	public Collection<IWirelessCaptureInterface> getWifiInterfaces() {
		return nativeWireless.listInterfaces();
	}

	private INativeWireless createNative() {
		switch(system.getOS()) {
		case OS_LINUX:
			return new LinuxNativeWireless(pcapFactory, this, system);
		case OS_OSX:
			return new OsxNativeWireless(pcapFactory, this);
		default:
			throw new IllegalStateException("No native wireless pcap implementation for current os");
		}
	}
	
	protected void activate(ComponentContext ctx) {
		initializeInterfaces();
	}
	
	protected void setSystemService(ISystemService system) {
		this.system = system;
	}
	
	protected void unsetSystemService(ISystemService system) {
		this.system = null;
	}
	
	protected void setPcapFactory(IPacketCaptureFactoryService factory) {
		pcapFactory = factory;	
	}
	
	protected void unsetPcapFactory(IPacketCaptureFactoryService factory) {
		
	}
	private void initializeInterfaces() {
		nativeWireless = createNative();
	}
	
	/* logging */

	private ILogger logger;
	
	protected void setLogManager(ILogManager logManager) {
		logger = logManager.getLogger("PacketCapture [Wireless]");
	}
	
	protected void unsetLogManager(ILogManager logManager) {
		logger = null;
	}

	public ILogger getLogger() {
		return logger;
	}
}
