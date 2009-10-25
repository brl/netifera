package com.netifera.platform.host.internal.filesystem;

import java.net.URI;

import com.netifera.platform.host.filesystem.IFileSystem;
import com.netifera.platform.host.filesystem.LocalFileSystem;
import com.netifera.platform.services.IServiceProvider;

public class LocalFileSystemProvider implements IServiceProvider {

	public IFileSystem create(URI url) {
		if (url.getScheme().equals("file"))
			return new LocalFileSystem();
		return null;
	}
	
	public Class<?> getType() {
		return LocalFileSystem.class;
	}
}
