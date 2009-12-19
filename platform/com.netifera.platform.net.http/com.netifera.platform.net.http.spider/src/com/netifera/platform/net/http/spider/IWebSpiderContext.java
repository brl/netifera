package com.netifera.platform.net.http.spider;

import java.net.URI;

import com.netifera.platform.api.log.ILogger;
import com.netifera.platform.util.addresses.inet.TCPSocketAddress;

public interface IWebSpiderContext {

	TCPSocketAddress getSocketAddress();
	URI getBaseURL();

	long getRealm();
	long getSpaceId();

	IWebSpider getSpider();
	
	ILogger getLogger();
}
