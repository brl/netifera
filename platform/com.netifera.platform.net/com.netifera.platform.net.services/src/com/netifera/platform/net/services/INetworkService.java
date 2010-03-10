package com.netifera.platform.net.services;

import java.io.Serializable;

import com.netifera.platform.util.addresses.inet.InternetSocketAddress;

public interface INetworkService extends Serializable {
	InternetSocketAddress getSocketAddress();
}
