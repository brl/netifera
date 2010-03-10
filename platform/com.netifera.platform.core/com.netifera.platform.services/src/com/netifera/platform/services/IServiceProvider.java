package com.netifera.platform.services;

import java.net.URI;

public interface IServiceProvider {
	Class<?> getType();
	Object create(URI url);
}
