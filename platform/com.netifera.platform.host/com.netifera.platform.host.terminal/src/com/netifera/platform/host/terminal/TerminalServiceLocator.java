package com.netifera.platform.host.terminal;

import java.net.URI;
import java.net.URISyntaxException;

import com.netifera.platform.api.model.IEntity;
import com.netifera.platform.services.ServiceLocator;

public class TerminalServiceLocator extends ServiceLocator {

	public TerminalServiceLocator(String url, IEntity host) throws URISyntaxException {
		super(url, host);
	}

	public TerminalServiceLocator(URI url, IEntity host) {
		super(url, host);
	}

	@Override
	public Class<?> getType() {
		return ITerminalService.class;
	}
}
