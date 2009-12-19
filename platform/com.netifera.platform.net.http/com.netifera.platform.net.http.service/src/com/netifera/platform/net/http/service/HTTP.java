package com.netifera.platform.net.http.service;

import org.apache.http.nio.protocol.HttpRequestExecutionHandler;
import org.apache.http.nio.reactor.IOReactorException;

import com.netifera.platform.net.services.NetworkService;
import com.netifera.platform.util.addresses.inet.InternetSocketAddress;
import com.netifera.platform.util.addresses.inet.TCPSocketAddress;

public class HTTP extends NetworkService {
	private static final long serialVersionUID = -4369719970659667081L;

	public HTTP(InternetSocketAddress address) {
		super(address);
	}

	@Override
	public TCPSocketAddress getSocketAddress() {
		return (TCPSocketAddress) super.getSocketAddress();
	}
	
	public HTTPClient createClient() {
		return new HTTPClient(getSocketAddress());
	}
	
	public AsynchronousHTTPClient createAsynchronousClient(HttpRequestExecutionHandler requestHandler) throws IOReactorException {
		return new AsynchronousHTTPClient(getSocketAddress(), null, requestHandler);
	}

	private boolean isSSL() {
		//HACK
		return getSocketAddress().getPort() == 443;
	}
	
	@Override
	public String getURIScheme() {
		return isSSL() ? "https" : "http";
	}
	
	public int getDefaultPort() {
		return isSSL() ? 443 : 80;
	}
}
