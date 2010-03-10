package com.netifera.platform.net.services.basic;

import java.io.IOException;
import java.net.SocketException;

import org.apache.commons.net.smtp.SMTPClient;

import com.netifera.platform.net.services.NetworkService;
import com.netifera.platform.util.addresses.inet.InternetSocketAddress;
import com.netifera.platform.util.addresses.inet.TCPSocketAddress;


public class SMTP extends NetworkService {
	private static final long serialVersionUID = 6595378589137067267L;

	public SMTP(InternetSocketAddress address) {
		super(address);
	}
	
	public TCPSocketAddress getSocketAddress() {
		return (TCPSocketAddress) super.getSocketAddress();
	}
	
	public SMTPClient createClient() throws SocketException, IOException {
		SMTPClient client = new SMTPClient();
		client.connect(getSocketAddress().getNetworkAddress().toInetAddress(), getSocketAddress().getPort());
		return client;
	}
	
	private boolean isSSL() {
		//HACK
		return getSocketAddress().getPort() == 465;
	}
	
	@Override
	public String getURIScheme() {
		return isSSL() ? "smtps" : "smtp";
	}
	
	public int getDefaultPort() {
		return isSSL() ? 465 : 25;
	}
}
