package com.netifera.platform.host.processes;

import java.net.URI;
import java.net.URISyntaxException;

import com.netifera.platform.api.model.IEntity;
import com.netifera.platform.services.ServiceLocator;

public class ProcessServiceLocator extends ServiceLocator {

	public ProcessServiceLocator(String url, IEntity host) throws URISyntaxException {
		super(url, host);
	}

	public ProcessServiceLocator(URI url, IEntity host) {
		super(url, host);
	}

	@Override
	public Class<?> getType() {
		return IProcessService.class;
	}
}
