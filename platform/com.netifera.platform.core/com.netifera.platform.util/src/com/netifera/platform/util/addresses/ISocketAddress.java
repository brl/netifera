package com.netifera.platform.util.addresses;

public abstract interface ISocketAddress extends IAbstractAddress {
	INetworkAddress getNetworkAddress();
	int getPort();
}
