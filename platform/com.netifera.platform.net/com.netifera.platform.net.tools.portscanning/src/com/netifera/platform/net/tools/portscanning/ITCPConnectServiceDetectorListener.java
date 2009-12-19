package com.netifera.platform.net.tools.portscanning;

import java.util.Map;

import com.netifera.platform.util.addresses.inet.TCPSocketAddress;

public interface ITCPConnectServiceDetectorListener {
	void connecting(TCPSocketAddress socketAddress);
	void unreachable(TCPSocketAddress socketAddress);
	void connected(TCPSocketAddress socketAddress);
	void serviceDetected(TCPSocketAddress socketAddress, Map<String,String> info);
	void finished(TCPSocketAddress socketAddress);
}
