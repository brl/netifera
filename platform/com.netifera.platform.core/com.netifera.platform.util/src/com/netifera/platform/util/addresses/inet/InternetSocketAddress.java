package com.netifera.platform.util.addresses.inet;

import java.io.Serializable;
import java.net.InetSocketAddress;

import com.netifera.platform.util.addresses.AddressFormatException;
import com.netifera.platform.util.addresses.ISocketAddress;

public abstract class InternetSocketAddress implements ISocketAddress, Serializable {
	private static final long serialVersionUID = -156398461713305277L;
	
	protected final InternetAddress address;
	protected final int port;

	/**
     * Returns an <code>InternetSocketAddress</code> object given the string.
     * 
	 * @param address the specified IP in decimal notation + ":" + port + "/" + protocol
	 * 
 	 * @exception AddressFormatException
	 */
	public static InternetSocketAddress fromString(String value) {
		String protocol = value.substring(value.length()-3);
		String addressAndPort = value.substring(0, value.length()-4);
		int port = Integer.parseInt(addressAndPort.substring(addressAndPort.lastIndexOf(":")+1));
		String address = addressAndPort.substring(0, addressAndPort.lastIndexOf(":"));
		if (protocol.equals("tcp")) {
			return new TCPSocketAddress(InternetAddress.fromString(address),port);
		} else if (protocol.equals("udp")) {
			return new UDPSocketAddress(InternetAddress.fromString(address),port);
		}
		throw new AddressFormatException("Invalid protocol: "+protocol);
	}

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
		return address.toStringLiteral() + ':' + port + '/' + getProtocol();
	}
}