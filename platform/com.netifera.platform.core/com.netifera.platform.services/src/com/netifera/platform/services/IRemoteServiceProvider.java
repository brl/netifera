package com.netifera.platform.services;

import java.net.URI;

import com.netifera.platform.api.probe.IProbe;

public interface IRemoteServiceProvider {
	Class<?> getType();
	Object create(URI url, IProbe probe);
}
