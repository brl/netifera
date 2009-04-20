package com.netifera.platform.net.wifi.internal.pcap.linux;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.netifera.platform.api.system.ISystemService;
import com.netifera.platform.net.pcap.IPacketCapture;
import com.netifera.platform.net.pcap.IPacketCaptureFactoryService;
import com.netifera.platform.net.wifi.internal.pcap.INativeWireless;
import com.netifera.platform.net.wifi.internal.pcap.WirelessCaptureInterface;
import com.netifera.platform.net.wifi.pcap.IWifiPacketCapture;
import com.netifera.platform.net.wifi.pcap.IWifiPacketCaptureFactory;
import com.netifera.platform.net.wifi.pcap.IWirelessCaptureInterface;

public class LinuxNativeWireless implements INativeWireless {
	final private ISystemService system;
	final private IPacketCaptureFactoryService pcapFactory;
	final private IWifiPacketCaptureFactory wifiFactory;
	final private static int IFNAMSIZ = 16;
	final private static int SIOCSIWMODE = 0x8B06;          /* set operation mode */
	final private static int SIOCGIWMODE = 0x8B07;       
	final private static int SIOCGIFHWADDR  =    0x8927;          /* Get hardware address */
	private final static int IW_MODE_MONITOR = 6;	
	
	
	public LinuxNativeWireless(IPacketCaptureFactoryService pcapFactory, IWifiPacketCaptureFactory wifiFactory, ISystemService system) {
		this.system = system;
		this.pcapFactory = pcapFactory;
		this.wifiFactory = wifiFactory;
	}
	
	public Collection<IWirelessCaptureInterface> listInterfaces() {
		File proc = new File("/proc/net/wireless");
		
		if(!proc.exists() || !proc.canRead())
			return Collections.emptyList();
		
		String[] wirelessLines;
		try {
			wirelessLines = readInterfaceInformation(proc);
		} catch (IOException e) {
			System.out.println("failed " + e.getMessage());
			return Collections.emptyList();
		
		}

		if(wirelessLines.length < 3) {
			return Collections.emptyList();
		}
		List<IWirelessCaptureInterface> result = new ArrayList<IWirelessCaptureInterface>();
		
		for(int i = 2; i < wirelessLines.length; i++) {
			String line = wirelessLines[i];
			System.out.println("line: " + line);
			int idx = line.indexOf(":");
			if(idx == -1) {
				// XXX
			}
			String dev = line.substring(0, idx).trim();
			if(pcapFactory.isInterfaceAvailable(dev)) {
				result.add(new WirelessCaptureInterface(wifiFactory, dev, true));
			}
		}
	
		return result;
		
	}
	
	private String[] readInterfaceInformation(File path) throws IOException {
		BufferedReader in = new BufferedReader(new FileReader(path));
		List<String> lines = new ArrayList<String>();
		
		String l = in.readLine();
		while(l != null) {
			lines.add(l);
			l = in.readLine();
		}
		return lines.toArray(new String[0]);
	}
	
	/*
	 * return a struct iwreq
	 */
	private byte[] deviceNameToIwreq(String device) {
		if(device.length() >= IFNAMSIZ) {
			System.out.println("Device name too long " + device);
			return null;
		}
		
		// struct iwreq
		final byte[] iwreq = new byte[32];
		for(int i = 0; i < device.length(); i++) {
			iwreq[i] = (byte) device.charAt(i);
		}
		
		return iwreq;
		
	}
	
	public boolean enableMonitorMode(IWifiPacketCapture pcap) {
		return setMonitorMode(pcap, true);
	}
	
	public boolean disableMonitorMode(IWifiPacketCapture pcap) {
		return setMonitorMode(pcap, false);
	}
	
	public boolean isMonitorModeEnabled(IWifiPacketCapture pcap) {
		
		byte[] iwreq = deviceNameToIwreq(pcap.getInterfaceName());
		if(system.syscall_ioctl(pcap.getFileDescriptor(), SIOCGIWMODE, iwreq, 32, 32) < 0) {
			System.out.println("ioctl failed " + system.getErrorMessage(system.getErrno()));
			return false;
		}
		int n = system.unpack32(iwreq, IFNAMSIZ);
		System.out.println("n = " + n);
		return n == IW_MODE_MONITOR;
	}

	private boolean setMonitorMode(IPacketCapture pcap, boolean enabled) {
		byte[] iwreq = deviceNameToIwreq(pcap.getInterfaceName());
		int mode = enabled? (IW_MODE_MONITOR) : (0);
		system.pack32(iwreq, IFNAMSIZ, mode);
		
		if(system.syscall_ioctl(pcap.getFileDescriptor(), SIOCSIWMODE, iwreq, 32, 0) < 0) {
			System.out.println("ioctl failed " + system.getErrorMessage(system.getErrno()));
			return false;
		}
		return true;		
	}


	public int getChannel(IWifiPacketCapture pcap) {
		// TODO Auto-generated method stub
		return 0;
	}

	public boolean setChannel(IWifiPacketCapture pcap, int channel) {
		// TODO Auto-generated method stub
		return false;
	}


}
