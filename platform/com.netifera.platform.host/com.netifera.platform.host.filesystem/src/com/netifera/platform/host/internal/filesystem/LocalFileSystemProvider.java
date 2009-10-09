package com.netifera.platform.host.internal.filesystem;

import java.net.URI;

import com.netifera.platform.host.filesystem.IFileSystem;
import com.netifera.platform.host.filesystem.IFileSystemProvider;
import com.netifera.platform.host.filesystem.LocalFileSystem;

public class LocalFileSystemProvider implements IFileSystemProvider {

	public IFileSystem create(URI url) {
		if (url.getScheme().equals("file"))
			return new LocalFileSystem();
		return null;
	}
	
	public String getScheme() {
		return "file";
	}
}
