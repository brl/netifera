package com.netifera.platform.services;

import java.net.URI;

import com.netifera.platform.api.probe.IProbe;

public interface IServiceFactory {
	Object create(Class<?> serviceType, URI url);
	Object create(Class<?> serviceType, URI url, IProbe probe);
	Object create(Class<?> serviceType, URI url, long probeId);
}
