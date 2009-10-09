package com.netifera.platform.host.filesystem;

import java.net.URI;


public interface IFileSystemProvider {
	String getScheme();
	IFileSystem create(URI url);
}
