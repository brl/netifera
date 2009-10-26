package com.netifera.platform.host.filesystem;

import java.net.URI;
import java.net.URISyntaxException;

import com.netifera.platform.services.ServiceLocator;

public class FileSystemLocator extends ServiceLocator {

	public FileSystemLocator(String url) throws URISyntaxException {
		super(url);
	}

	public FileSystemLocator(URI url) {
		super(url);
	}

	@Override
	public Class<?> getType() {
		return IFileSystem.class;
	}
}
