package com.netifera.platform.host.processes;

import java.net.URI;
import java.net.URISyntaxException;

import com.netifera.platform.services.ServiceLocator;

public class ProcessServiceLocator extends ServiceLocator {

	public ProcessServiceLocator(String url) throws URISyntaxException {
		super(url);
	}

	public ProcessServiceLocator(URI url) {
		super(url);
	}

	@Override
	public Class<?> getType() {
		return IProcessService.class;
	}
}
