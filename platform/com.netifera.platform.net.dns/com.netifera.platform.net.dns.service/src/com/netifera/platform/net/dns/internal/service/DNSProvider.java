package com.netifera.platform.net.dns.internal.service;

import com.netifera.platform.net.dns.service.DNS;
import com.netifera.platform.net.services.INetworkService;
import com.netifera.platform.net.services.INetworkServiceProvider;
import com.netifera.platform.util.addresses.inet.InternetSocketAddress;

public class DNSProvider implements INetworkServiceProvider {
	
	public Class<? extends INetworkService> getServiceClass() {
		return DNS.class;
	}

	public String getServiceName() {
		return "DNS";
	}

	public DNS create(InternetSocketAddress address) {
		return new DNS(address);
	}
}
