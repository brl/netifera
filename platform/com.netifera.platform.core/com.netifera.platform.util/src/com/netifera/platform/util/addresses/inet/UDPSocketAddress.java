package com.netifera.platform.util.addresses.inet;

import java.net.InetSocketAddress;

public class UDPSocketAddress extends InternetSocketAddress {
	private static final long serialVersionUID = 2047894733925186379L;

	public UDPSocketAddress(InternetAddress address, int port) {
		super(address, port);
	}

	public UDPSocketAddress(InetSocketAddress socketAddress) {
		this(InternetAddress.fromInetAddress(socketAddress.getAddress()), socketAddress.getPort());
	}

	@Override
	public String getProtocol() {
		return "udp";
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof UDPSocketAddress))
			return false;
		return ((UDPSocketAddress)o).address.equals(address) && ((UDPSocketAddress)o).port == port;
	}
	
	@Override
	public int hashCode() {
		return address.hashCode() ^ port ^ 0xFFFFFFFF;
	}
}
