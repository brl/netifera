package com.netifera.platform.net.http.internal.service;

import com.netifera.platform.net.http.service.HTTP;
import com.netifera.platform.net.services.INetworkService;
import com.netifera.platform.net.services.INetworkServiceProvider;
import com.netifera.platform.util.addresses.inet.InternetSocketAddress;

public class HTTPProvider implements INetworkServiceProvider {

	public INetworkService create(InternetSocketAddress address) {
		return new HTTP(address);
	}

	public Class<? extends INetworkService> getServiceClass() {
		return HTTP.class;
	}

	public String getServiceName() {
		return "HTTP";
	}
}
