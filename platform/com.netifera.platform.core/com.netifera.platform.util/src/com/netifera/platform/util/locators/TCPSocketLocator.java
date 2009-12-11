package com.netifera.platform.util.locators;

import java.net.InetSocketAddress;

import com.netifera.platform.util.addresses.inet.InternetAddress;


public class TCPSocketLocator implements ISocketLocator {
	private static final long serialVersionUID = 2723774071711129034L;
	
	private final InternetAddress address;
	private final int port;
	
	public TCPSocketLocator(InetSocketAddress socketAddress) {
		this(InternetAddress.fromInetAddress(socketAddress.getAddress()), socketAddress.getPort());
	}

	public TCPSocketLocator(InternetAddress address, int port) {
		this.address = address;
		this.port = port;
	}
	
	public InternetAddress getAddress() {
		return address;
	}
	
	public int getPort() {
		return port;
	}
	
	public String getProtocol() {
		return "tcp";
	}
	
	public InetSocketAddress toInetSocketAddress() {
		return new InetSocketAddress(address.toInetAddress(), port);
	}
	
	/*
	static TCPSocketLocator fromInetSocketAddress(InetSocketAddress sockaddr) {
		return new TCPSocketLocator(InternetAddress.fromInetAddress(
				sockaddr.getAddress()), sockaddr.getPort());
	}
	*/
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof TCPSocketLocator))
			return false;
		return ((TCPSocketLocator)o).address.equals(address) && ((TCPSocketLocator)o).port == port; 
	}
	
	@Override
	public int hashCode() {
		return address.hashCode()+port;
	}
	
	@Override
	public String toString() {
		return address.toStringLiteral() + ':' + Integer.toString(port)
			+ '/' + getProtocol();
	}
}
