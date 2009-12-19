package com.netifera.platform.util.addresses.inet;

import java.net.InetSocketAddress;

public class TCPSocketAddress extends InternetSocketAddress {
	private static final long serialVersionUID = 2047894733925186379L;

	public TCPSocketAddress(InternetAddress address, int port) {
		super(address, port);
	}

	public TCPSocketAddress(InetSocketAddress socketAddress) {
		this(InternetAddress.fromInetAddress(socketAddress.getAddress()), socketAddress.getPort());
	}

	@Override
	public String getProtocol() {
		return "tcp";
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof TCPSocketAddress))
			return false;
		return ((TCPSocketAddress)o).address.equals(address) && ((TCPSocketAddress)o).port == port;
	}
	
	@Override
	public int hashCode() {
		return address.hashCode() ^ port;
	}
}
