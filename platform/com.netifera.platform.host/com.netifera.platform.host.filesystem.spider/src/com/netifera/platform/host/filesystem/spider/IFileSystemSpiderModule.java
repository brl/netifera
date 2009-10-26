package com.netifera.platform.host.filesystem.spider;

import com.netifera.platform.host.filesystem.File;

public interface IFileSystemSpiderModule {
	String getName();
	
	boolean isCompatible(String system);

	void start(IFileSystemSpiderContext context);
	void handle(IFileSystemSpiderContext context, File file, IFileContent content);
	void stop(IFileSystemSpiderContext context);
}
