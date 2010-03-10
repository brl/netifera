package com.netifera.platform.net.services.basic;

import java.io.IOException;
import java.net.SocketException;
import java.util.Collections;
import java.util.List;

import org.apache.commons.net.telnet.TelnetClient;

import com.netifera.platform.net.services.NetworkService;
import com.netifera.platform.net.services.auth.IAuthenticable;
import com.netifera.platform.net.services.credentials.Credential;
import com.netifera.platform.net.services.credentials.UsernameAndPassword;
import com.netifera.platform.util.addresses.inet.InternetSocketAddress;
import com.netifera.platform.util.addresses.inet.TCPSocketAddress;


public class Telnet extends NetworkService implements IAuthenticable {
	private static final long serialVersionUID = -1559740994317977398L;

	public Telnet(InternetSocketAddress address) {
		super(address);
	}
	
	public TCPSocketAddress getSocketAddress() {
		return (TCPSocketAddress) super.getSocketAddress();
	}

	public boolean isAuthenticableWith(Credential credential) {
		return credential instanceof UsernameAndPassword;
	}

	public List<Credential> defaultCredentials() {
		return Collections.emptyList();
	}
	
	public TelnetClient createClient() throws SocketException, IOException {
		TelnetClient client = new TelnetClient();
		client.connect(getSocketAddress().getNetworkAddress().toInetAddress(), getSocketAddress().getPort());
		return client;
	}
	
	@Override
	public String getURIScheme() {
		return isSSL() ? "telnets" : "telnet";
	}

	private boolean isSSL() {
		//HACK
		return getSocketAddress().getPort() == 992;
	}

	public int getDefaultPort() {
		return isSSL() ? 992 : 23;
	}
}
