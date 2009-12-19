package com.netifera.platform.util.addresses.inet;

import java.io.Serializable;
import java.net.InetSocketAddress;

import com.netifera.platform.util.addresses.ISocketAddress;

public abstract class InternetSocketAddress implements ISocketAddress, Serializable {
	private static final long serialVersionUID = -156398461713305277L;
	
	protected final InternetAddress address;
	protected final int port;
	
	public InternetSocketAddress(InternetAddress address, int port) {
		this.address = address;
		this.port = port;
	}
	
	public InternetAddress getNetworkAddress() {
		return address;
	}

	public int getPort() {
		return port;
	}

	public abstract String getProtocol();
	
	public int getDataSize() {
		return address.getDataSize()+16;
	}

	public byte[] toBytes() {
		// TODO Auto-generated method stub
		return null;
	}

	public InetSocketAddress toInetSocketAddress() {
		return new InetSocketAddress(address.toInetAddress(), port);
	}
	
	@Override
	public String toString() {
		return address.toStringLiteral() + ':' + Integer.toString(port) + '/' + getProtocol();
	}
}
