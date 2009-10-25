package com.netifera.platform.net.internal.services.basic;

import java.net.URI;

import com.netifera.platform.host.filesystem.IFileSystem;
import com.netifera.platform.net.services.basic.FTPFileSystem;
import com.netifera.platform.services.IServiceProvider;

public class FTPFileSystemProvider implements IServiceProvider {

	public IFileSystem create(URI url) {
		if (url.getScheme().equals("ftp"))
			return new FTPFileSystem(url);
		return null;
	}

	public Class<?> getType() {
		return FTPFileSystem.class;
	}
}
