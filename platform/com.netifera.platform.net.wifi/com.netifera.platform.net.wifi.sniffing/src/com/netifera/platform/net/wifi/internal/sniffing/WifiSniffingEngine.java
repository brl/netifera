package com.netifera.platform.net.wifi.internal.sniffing;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.osgi.service.component.ComponentContext;

import com.netifera.platform.api.log.ILogManager;
import com.netifera.platform.api.log.ILogger;
import com.netifera.platform.net.packets.IPacketHeader;
import com.netifera.platform.net.packets.link.ARP;
import com.netifera.platform.net.packets.tcpip.IPv4;
import com.netifera.platform.net.packets.tcpip.IPv6;
import com.netifera.platform.net.pcap.ICaptureInterface;
import com.netifera.platform.net.sniffing.ICaptureFileInterface;
import com.netifera.platform.net.sniffing.IPacketFilter;
import com.netifera.platform.net.sniffing.IPacketSniffer;
import com.netifera.platform.net.sniffing.ISnifferHandle;
import com.netifera.platform.net.sniffing.stream.IBlockSniffer;
import com.netifera.platform.net.sniffing.stream.IBlockSnifferHandle;
import com.netifera.platform.net.sniffing.stream.IStreamSniffer;
import com.netifera.platform.net.sniffing.stream.IStreamSnifferHandle;
import com.netifera.platform.net.sniffing.util.CaptureFileInterface;
import com.netifera.platform.net.sniffing.util.IBasicInterfaceManager;
import com.netifera.platform.net.sniffing.util.InterfaceManager;
import com.netifera.platform.net.wifi.packets.WiFiFrame;
import com.netifera.platform.net.wifi.pcap.IWifiPacketCaptureFactory;
import com.netifera.platform.net.wifi.pcap.IWirelessCaptureInterface;
import com.netifera.platform.net.wifi.sniffing.IWifiSniffingEngine;

public class WifiSniffingEngine implements IWifiSniffingEngine {

	private final static int DEFAULT_SNAPLEN = 65535;
	private final static int DEFAULT_TIMEOUT = 500;
	
	private final int snaplen = DEFAULT_SNAPLEN;
	private final int timeout = DEFAULT_TIMEOUT;
	private final boolean promiscuous = true;
	
	private IWifiPacketCaptureFactory wifiFactory;
	
	private final Map<IWirelessCaptureInterface, WifiInterfaceManager> interfaces =
		new HashMap<IWirelessCaptureInterface, WifiInterfaceManager>();
	
	private final Map<CaptureFileInterface, WifiInterfaceManager> captureFileInterfaces =
		new HashMap<CaptureFileInterface, WifiInterfaceManager>();
	
	public Collection<ICaptureInterface> getInterfaces() {
		System.out.println("Getting wifi interfaces " + wifiFactory.getWifiInterfaces());
		return new HashSet<ICaptureInterface>(wifiFactory.getWifiInterfaces());
	}
	
	public Collection<IWirelessCaptureInterface> getWifiInterfaces() {
		return wifiFactory.getWifiInterfaces();
	}
	
	public ISnifferHandle createWifiHandle(IWirelessCaptureInterface iface,
			IPacketFilter filter, IPacketSniffer<WiFiFrame> sniffer) {
		return getManagerForInterface(iface).createWifiHandle(filter, sniffer);
	}
	
	
	private WifiInterfaceManager getManagerForInterface(ICaptureInterface iface) {
		if (iface == null) {
			throw new IllegalArgumentException("capture interface is null");
		}
		
		if (!interfaces.containsKey(iface)) {
			throw new IllegalArgumentException(
					"No interface found for specified handle : " + iface);
		}
		
		return interfaces.get(iface);
	}
	
	public void removeCaptureFileInterface(CaptureFileInterface iface) {
		// TODO Auto-generated method stub
		
	}

	
	protected void setWifiCaptureFactory(IWifiPacketCaptureFactory factory) {
		wifiFactory = factory;
	}
	
	protected void unsetWifiCaptureFactory(IWifiPacketCaptureFactory factory) {
		wifiFactory = null;
	}
	
	protected void activate(ComponentContext ctx) {
		initializeInterfaces();
	}
	
	protected void deactivate(ComponentContext ctx) {
		disposeInterfaces();
	}
	
	private void initializeInterfaces() {
		for(IWirelessCaptureInterface iface: wifiFactory.getWifiInterfaces()) {
			final IBasicInterfaceManager manager = InterfaceManager.createBasic(new WirelessRawManager(this, iface));
			interfaces.put(iface, new WifiInterfaceManager(manager, this, iface));
		}
	}
	
	private void disposeInterfaces() {
		for(ICaptureInterface iface : interfaces.keySet()) {
			interfaces.get(iface).dispose();
		}
		interfaces.clear();
	}

	/* logging */

	private ILogger logger;
	
	protected void setLogManager(ILogManager logManager) {
		logger = logManager.getLogger("WiFi Sniffing Engine");
	}
	
	protected void unsetLogManager(ILogManager logManager) {
		logger = null;
	}


	public ILogger getLogger() {
		return logger;
	}

	public ISnifferHandle createArpHandle(ICaptureInterface iface,
			IPacketFilter filter, IPacketSniffer<ARP> sniffer) {
		return getManagerForInterface(iface).createArpHandle(filter, sniffer);
	}

	public ICaptureFileInterface createCaptureFileInterface(String path) {
		final CaptureFileInterface iface = new CaptureFileInterface(path, this);
		if(iface.isValid()) {
			final IBasicInterfaceManager basicManager = InterfaceManager.createCaptureFileManager(this, iface);
			final WifiInterfaceManager wifiManager = new WifiInterfaceManager(basicManager, this, iface);
			captureFileInterfaces.put(iface, wifiManager);
		}
		return iface;
	}

	public ISnifferHandle createIPv4Handle(ICaptureInterface iface,
			IPacketFilter filter, IPacketSniffer<IPv4> sniffer) {
		return getManagerForInterface(iface).createIPv4Handle(filter, sniffer);
	}


	public ISnifferHandle createIPv6Handle(ICaptureInterface iface,
			IPacketFilter filter, IPacketSniffer<IPv6> sniffer) {
		return getManagerForInterface(iface).createIPv6Handle(filter, sniffer);
	}


	public ISnifferHandle createRawHandle(ICaptureInterface iface,
			IPacketFilter filter, IPacketSniffer<IPacketHeader> sniffer) {
		return getManagerForInterface(iface).createRawHandle(filter, sniffer);
	}

	public IBlockSnifferHandle createTcpBlockHandle(ICaptureInterface iface,
			IPacketFilter filter, IBlockSniffer sniffer) {
		return getManagerForInterface(iface).createTCPBlockHandle(filter, sniffer);
	}

	public IStreamSnifferHandle createTcpStreamHandle(ICaptureInterface iface,
			IPacketFilter filter, IStreamSniffer sniffer) {
		return getManagerForInterface(iface).createTCPStreamHandle(filter, sniffer);
	}

	
	public boolean getPromiscuous() {
		return promiscuous;
	}

	public int getSnaplen() {
		return snaplen;
	}

	public int getTimeout() {
		return timeout;
	}

}
