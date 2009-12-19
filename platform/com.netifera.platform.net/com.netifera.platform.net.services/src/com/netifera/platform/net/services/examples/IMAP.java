package com.netifera.platform.net.services.examples;

import java.util.Collections;
import java.util.List;

import com.netifera.platform.net.services.NetworkService;
import com.netifera.platform.net.services.auth.IAuthenticable;
import com.netifera.platform.net.services.credentials.Credential;
import com.netifera.platform.net.services.credentials.UsernameAndPassword;
import com.netifera.platform.util.addresses.inet.InternetSocketAddress;
import com.netifera.platform.util.addresses.inet.TCPSocketAddress;

public class IMAP extends NetworkService implements IAuthenticable {
	private static final long serialVersionUID = -5683702927755879994L;

	public IMAP(InternetSocketAddress address) {
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

	private boolean isSSL() {
		//HACK
		return getSocketAddress().getPort() == 993;
	}
	
	@Override
	public String getURIScheme() {
		return isSSL() ? "imaps" : "imap";
	}
	
	public int getDefaultPort() {
		return isSSL() ? 993 : 143;  // IMAP2, IMAP2bis,  IMAP4rev1
	}
}
