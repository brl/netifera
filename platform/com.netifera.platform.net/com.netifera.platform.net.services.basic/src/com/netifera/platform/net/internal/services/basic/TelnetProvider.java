package com.netifera.platform.net.internal.services.basic;

import com.netifera.platform.net.services.INetworkService;
import com.netifera.platform.net.services.INetworkServiceProvider;
import com.netifera.platform.net.services.basic.Telnet;
import com.netifera.platform.util.addresses.inet.InternetSocketAddress;

public class TelnetProvider implements INetworkServiceProvider {

	public Class<? extends INetworkService> getServiceClass() {
		return Telnet.class;
	}

	public String getServiceName() {
		return "Telnet";
	}

	public Telnet create(InternetSocketAddress address) {
		return new Telnet(address);
	}
}
