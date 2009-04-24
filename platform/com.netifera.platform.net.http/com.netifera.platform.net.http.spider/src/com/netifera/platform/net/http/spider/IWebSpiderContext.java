package com.netifera.platform.net.http.spider;

import java.net.URI;

import com.netifera.platform.api.log.ILogger;
import com.netifera.platform.util.locators.TCPSocketLocator;

public interface IWebSpiderContext {

	TCPSocketLocator getSocketLocator();
	URI getBaseURL();

	long getRealm();
	long getSpaceId();

	IWebSpider getSpider();
	
	ILogger getLogger();
}
