package com.netifera.platform.host.filesystem.spider.modules;

import com.netifera.platform.host.filesystem.File;
import com.netifera.platform.host.filesystem.spider.IFileContent;
import com.netifera.platform.host.filesystem.spider.IFileSystemSpiderContext;
import com.netifera.platform.host.filesystem.spider.IFileSystemSpiderModule;

public class HomesHarvester implements IFileSystemSpiderModule {

	public String getName() {
		return "Harvest User Home Directories";
	}

	public void handle(IFileSystemSpiderContext context, File file, IFileContent content) {
		if (file.getAbsolutePath().equals("/etc/passwd")) {
//			content.g
		}
	}

	public boolean isCompatible(String system) {
		return !system.matches("(?i)windows|win32");
	}

	public void start(IFileSystemSpiderContext context) {
		context.getSpider().fetch("/etc/passwd");
	}

	public void stop(IFileSystemSpiderContext context) {
	}
}
