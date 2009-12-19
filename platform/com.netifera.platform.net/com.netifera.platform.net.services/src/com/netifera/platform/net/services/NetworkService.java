package com.netifera.platform.net.services;

import java.util.Locale;

import com.netifera.platform.util.addresses.inet.InternetSocketAddress;

public abstract class NetworkService implements INetworkService {
	private static final long serialVersionUID = 6240506251424087662L;
	
	final private InternetSocketAddress address;

	public NetworkService(final InternetSocketAddress address) {
		this.address = address;
	}
	
	public InternetSocketAddress getSocketAddress() {
		return address;
	}
	
	@Override
	public String toString() {
		return getURI();
	}
	
	public abstract int getDefaultPort();
	
	/*
	 * following code related to URIs
	 * 
	 * See: RFC 3986
	 * 
	 */
	
	public String getURIScheme() {
		return getClass().getSimpleName().toLowerCase(Locale.ENGLISH);
	}
	
	public String getURIHost() {
		return getSocketAddress().getNetworkAddress().toStringLiteral();
	}
	
	public String getURIHost(String hostname) {
		if (hostname != null && hostname.length() > 0) {
			return hostname;
		}
		return getSocketAddress().getNetworkAddress().toStringLiteral();
	}
	
	public String getURIHostPort() {
		return getURIHostPort(getURIHost());
	}
	
	public String getURIHostPort(String hostname) {
		StringBuilder sb = new StringBuilder(256);
		sb.append(getURIHost(hostname));			
		int port = getSocketAddress().getPort();
		if (port != getDefaultPort()) {
			sb.append(':');
			sb.append(Integer.valueOf(port));
		}
		return sb.toString();
	}
		
	public String getURIAuthority() {
		return getURIAuthority(getURIHost());
	}
	
	public String getURIAuthority(String hostname) {
		return getURIAuthorityPrefix() + getURIHostPort(hostname);
	}
	
	/* to overwrite */
	protected String getURIAuthorityPrefix() {
		// TODO check IAuthenticable ?
		return "";
	}
	
	public String getURI() {
		return getURI(getURIHost());
	}
	
	public String getURI(String hostname) {
		StringBuilder sb = new StringBuilder(128);
		sb.append(getURIScheme());
		sb.append("://");
		sb.append(getURIAuthority(hostname));
		return sb.append('/').toString();
	}
}
