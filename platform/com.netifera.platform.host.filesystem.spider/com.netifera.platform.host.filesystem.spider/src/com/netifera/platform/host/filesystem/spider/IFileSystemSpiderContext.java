package com.netifera.platform.host.filesystem.spider;

import com.netifera.platform.api.log.ILogger;
import com.netifera.platform.util.addresses.inet.InternetAddress;

public interface IFileSystemSpiderContext {
	InternetAddress getHostAddress();
	long getRealm();
	long getSpaceId();

	IFileSystemSpider getSpider();
	
	ILogger getLogger();
}
