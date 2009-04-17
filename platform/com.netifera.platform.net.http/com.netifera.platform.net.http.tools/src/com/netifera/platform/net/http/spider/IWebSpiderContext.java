package com.netifera.platform.net.http.spider;

import java.net.URI;

import com.netifera.platform.api.tools.IToolContext;
import com.netifera.platform.util.locators.TCPSocketLocator;

public interface IWebSpiderContext {

	TCPSocketLocator getLocator();
	URI getBaseURL();

	long getRealm();
	long getSpaceId();

	IToolContext getToolContext();
	
	IWebSpider getSpider();
}
