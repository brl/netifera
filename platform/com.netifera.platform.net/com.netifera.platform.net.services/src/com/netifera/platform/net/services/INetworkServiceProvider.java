package com.netifera.platform.net.services;

import com.netifera.platform.util.addresses.inet.InternetSocketAddress;


public interface INetworkServiceProvider {
	String getServiceName();
	Class<? extends INetworkService> getServiceClass();
	INetworkService create(InternetSocketAddress address);
}
