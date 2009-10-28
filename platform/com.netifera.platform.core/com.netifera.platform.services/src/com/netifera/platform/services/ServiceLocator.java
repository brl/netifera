package com.netifera.platform.services;

import java.net.URI;
import java.net.URISyntaxException;

import com.netifera.platform.api.model.IEntity;
import com.netifera.platform.api.probe.IProbe;
import com.netifera.platform.services.internal.Activator;

abstract public class ServiceLocator {
	final private URI url;
	final private IEntity host;

	public ServiceLocator(String url, IEntity host) throws URISyntaxException {
		this(new URI(url), host);
	}
	
	public ServiceLocator(URI url, IEntity host) {
		this.url = url;
		this.host = host;
	}

	abstract public Class<?> getType();
	
	public URI getURL() {
		return url;
	}
	
	public IEntity getHost() {
		return host;
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
