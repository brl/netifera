package com.netifera.platform.host.filesystem;

import java.net.URI;
import java.net.URISyntaxException;

import com.netifera.platform.api.model.IEntity;
import com.netifera.platform.services.ServiceLocator;

public class FileSystemServiceLocator extends ServiceLocator {

	public FileSystemServiceLocator(String url, IEntity host) throws URISyntaxException {
		super(url, host);
	}

	public FileSystemServiceLocator(URI url, IEntity host) {
		super(url, host);
	}

	@Override
	public Class<?> getType() {
		return IFileSystem.class;
	}
}
