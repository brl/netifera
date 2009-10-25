package com.netifera.platform.net.ssh.internal.filesystem;

import java.net.URI;

import com.netifera.platform.host.filesystem.IFileSystem;
import com.netifera.platform.net.ssh.filesystem.SFTPFileSystem;
import com.netifera.platform.services.IServiceProvider;

public class SFTPFileSystemProvider implements IServiceProvider {

	public IFileSystem create(URI url) {
		if (url.getScheme().equals("sftp"))
			return new SFTPFileSystem(url);
		return null;
	}
	
	public Class<?> getType() {
		return SFTPFileSystem.class;
	}
}
