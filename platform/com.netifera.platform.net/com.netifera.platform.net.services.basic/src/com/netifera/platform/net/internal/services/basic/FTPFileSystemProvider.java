package com.netifera.platform.net.internal.services.basic;

import java.net.URI;

import com.netifera.platform.host.filesystem.IFileSystem;
import com.netifera.platform.host.filesystem.IFileSystemProvider;
import com.netifera.platform.net.services.basic.FTPFileSystem;

public class FTPFileSystemProvider implements IFileSystemProvider {

	public IFileSystem create(URI url) {
		if (url.getScheme().equals("ftp"))
			return new FTPFileSystem(url);
		return null;
	}
	
	public String getScheme() {
		return "ftp";
	}
}
