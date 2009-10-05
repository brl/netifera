package com.netifera.platform.net.cifs;

import com.netifera.platform.net.services.credentials.Credential;

public class WindowsCredential implements Credential {
	private static final long serialVersionUID = -5085053314243736622L;
	
	final private String username;
	final private String password;
	final private String domain;

	public WindowsCredential(String username, String password, String domain) {
		this.username = username;
		this.password = password;
		this.domain = domain;
	}
	
	public String getUsernameString() {
		return username;
	}
	
	public String getPasswordString() {
		return password;
	}
	
	public String getDomainString() {
		return domain;
	}
}
