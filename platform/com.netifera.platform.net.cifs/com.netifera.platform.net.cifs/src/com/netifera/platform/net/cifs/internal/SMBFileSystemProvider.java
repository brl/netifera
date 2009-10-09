package com.netifera.platform.net.cifs.internal;

import java.net.URI;

import com.netifera.platform.host.filesystem.IFileSystem;
import com.netifera.platform.host.filesystem.IFileSystemProvider;
import com.netifera.platform.net.cifs.filesystem.SMBFileSystem;

public class SMBFileSystemProvider implements IFileSystemProvider {

	public IFileSystem create(URI url) {
		if (url.getScheme().equals("smb"))
			return new SMBFileSystem(url);
		return null;
	}
	
	public String getScheme() {
		return "smb";
	}
}
