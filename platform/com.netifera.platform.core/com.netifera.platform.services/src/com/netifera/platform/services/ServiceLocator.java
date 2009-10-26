package com.netifera.platform.services;

import java.net.URI;
import java.net.URISyntaxException;

import com.netifera.platform.api.probe.IProbe;
import com.netifera.platform.services.internal.Activator;

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

	public Object create(IProbe probe) {
		return Activator.getInstance().getServiceFactory().create(getType(), getURL(), probe);
	}

	public Object create() {
		return Activator.getInstance().getServiceFactory().create(getType(), getURL());
	}
	
	public String toString() {
		return url.toString();
	}
}
