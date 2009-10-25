package com.netifera.platform.net.cifs.internal;

import java.net.URI;

import com.netifera.platform.host.filesystem.IFileSystem;
import com.netifera.platform.net.cifs.filesystem.SMBFileSystem;
import com.netifera.platform.services.IServiceProvider;

public class SMBFileSystemProvider implements IServiceProvider {

	public IFileSystem create(URI url) {
		if (url.getScheme().equals("smb"))
			return new SMBFileSystem(url);
		return null;
	}

	public Class<?> getType() {
		return SMBFileSystem.class;
	}
}
