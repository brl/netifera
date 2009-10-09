package com.netifera.platform.net.ssh.internal.filesystem;

import java.net.URI;

import com.netifera.platform.host.filesystem.IFileSystem;
import com.netifera.platform.host.filesystem.IFileSystemProvider;
import com.netifera.platform.net.ssh.filesystem.SFTPFileSystem;

public class SFTPFileSystemProvider implements IFileSystemProvider {

	public IFileSystem create(URI url) {
		if (url.getScheme().equals("sftp"))
			return new SFTPFileSystem(url);
		return null;
	}
	
	public String getScheme() {
		return "sftp";
	}
}
