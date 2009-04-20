package com.netifera.platform.net.wifi.internal.pcap.osx;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.netifera.platform.net.pcap.Datalink;
import com.netifera.platform.net.pcap.ICaptureHeader;
import com.netifera.platform.net.pcap.ICaptureInterface;
import com.netifera.platform.net.pcap.IPacketCapture;
import com.netifera.platform.net.pcap.IPacketCaptureFactoryService;
import com.netifera.platform.net.pcap.IPacketHandler;
import com.netifera.platform.net.wifi.internal.pcap.INativeWireless;
import com.netifera.platform.net.wifi.internal.pcap.WirelessCaptureInterface;
import com.netifera.platform.net.wifi.pcap.IWifiPacketCapture;
import com.netifera.platform.net.wifi.pcap.IWifiPacketCaptureFactory;
import com.netifera.platform.net.wifi.pcap.IWirelessCaptureInterface;

public class OsxNativeWireless implements INativeWireless {

	private final IPacketCaptureFactoryService pcapFactory;
	private final IWifiPacketCaptureFactory wifiFactory;
	public OsxNativeWireless(IPacketCaptureFactoryService pcapFactory, IWifiPacketCaptureFactory wifiFactory) {
		this.pcapFactory = pcapFactory;
		this.wifiFactory = wifiFactory;
	}
	
	private final static IPacketHandler nullHandler = new IPacketHandler() {
		public void handlePacket(ByteBuffer packetData,	ICaptureHeader header) {}
	};
	
	
	public boolean isWirelessInterface(ICaptureInterface iface) {
		

		/* Some arbitrary argument values because we just want to temporarily
		 * open the packet capture device so we can query which types
		 * of datalink are supported.
		 */
		final IPacketCapture pcap = pcapFactory.create(iface, 1000, false, 1000, nullHandler);

		if(!pcap.open()) {
			return false;
		}
		
		for(Datalink dlt : pcap.getDltList()) {
			if(isMonitorDatalink(dlt)) {
				pcap.close();
				return true;
			}
		}
		
		pcap.close();
		return false;
	}
	
	private boolean isMonitorDatalink(Datalink dlt) {
		switch(dlt) {
		case DLT_IEEE802_11:
		case DLT_IEEE802_11_RADIO:
		case DLT_IEEE802_11_RADIO_AVS:
			return true;
			
		default:
			return false;
		}
		
	}

	
	public boolean disableMonitorMode(IWifiPacketCapture pcap) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean enableMonitorMode(IWifiPacketCapture pcap) {
		boolean found = false;
		for(Datalink dlt : pcap.getDltList()) {
			if(isMonitorDatalink(dlt))
				found = true;
			
		}
		if(!found) {
			pcap.setError("Wireless datalink not found");
			return false;
		}
		
		
		pcap.setDataLink(Datalink.DLT_IEEE802_11);
		pcap.setDataLink(Datalink.DLT_IEEE802_11_RADIO);
		
	
		return true;
	}

	public int getChannel(IWifiPacketCapture pcap) {
		pcap.setError("Getting current channel not yet supported");
		return -1;
	}

	public Collection<IWirelessCaptureInterface> listInterfaces() {
		List<IWirelessCaptureInterface> wirelessInterfaces = new ArrayList<IWirelessCaptureInterface>();
		for(ICaptureInterface iface : pcapFactory.getCurrentInterfaces()) {
			if(isWirelessInterface(iface)) {
				System.out.println("Adding wireless interface " + iface);
				wirelessInterfaces.add(new WirelessCaptureInterface(wifiFactory, iface));
			}
		}
		return wirelessInterfaces;
	}

	public boolean setChannel(IWifiPacketCapture pcap, int channel) {
		pcap.setError("Setting channel not yet supported");
		return false;
	}

}
