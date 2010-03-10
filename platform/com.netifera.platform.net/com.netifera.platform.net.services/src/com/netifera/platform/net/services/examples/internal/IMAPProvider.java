package com.netifera.platform.net.services.examples.internal;

import com.netifera.platform.net.services.INetworkService;
import com.netifera.platform.net.services.INetworkServiceProvider;
import com.netifera.platform.net.services.examples.IMAP;
import com.netifera.platform.util.addresses.inet.InternetSocketAddress;

public class IMAPProvider implements INetworkServiceProvider {

	public Class<? extends INetworkService> getServiceClass() {
		return IMAP.class;
	}

	public String getServiceName() {
		return "IMAP";
	}

	public IMAP create(InternetSocketAddress address) {
		return new IMAP(address);
	}

}
