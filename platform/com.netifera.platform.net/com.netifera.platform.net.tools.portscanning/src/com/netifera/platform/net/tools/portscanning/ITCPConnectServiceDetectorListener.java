package com.netifera.platform.net.tools.portscanning;

import java.util.Map;

import com.netifera.platform.util.locators.TCPSocketLocator;

public interface ITCPConnectServiceDetectorListener {
	void connecting(TCPSocketLocator locator);
	void unreachable(TCPSocketLocator locator);
	void connected(TCPSocketLocator locator);
	void serviceDetected(TCPSocketLocator locator, Map<String,String> info);
	void finished(TCPSocketLocator locator);
}
