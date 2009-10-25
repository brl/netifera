package com.netifera.platform.services;

import java.net.URI;

abstract public class ServiceLocator {
	final private URI url;
	
	public ServiceLocator(URI url) {
		this.url = url;
	}

	abstract public Class<?> getType();
	
	public URI getURL() {
		return url;
	}
	
	public String toString() {
		return url.toString();
	}
}
