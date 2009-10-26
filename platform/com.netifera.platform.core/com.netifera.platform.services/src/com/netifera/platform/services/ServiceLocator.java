package com.netifera.platform.services;

import java.net.URI;
import java.net.URISyntaxException;

abstract public class ServiceLocator {
	final private URI url;

	public ServiceLocator(String url) throws URISyntaxException {
		this(new URI(url));
	}
	
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
