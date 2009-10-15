package com.netifera.platform.net.services.detection;

import com.netifera.platform.util.PortSet;

public interface INetworkServiceTrigger {
	String getName();
	int getTimeout();
	String getProtocol();
	PortSet getPorts();
	byte[] getBytes();
}
