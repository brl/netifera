package com.netifera.platform.host.terminal;

import java.net.URI;
import java.net.URISyntaxException;

import com.netifera.platform.services.ServiceLocator;

public class TerminalServiceLocator extends ServiceLocator {

	public TerminalServiceLocator(String url) throws URISyntaxException {
		super(url);
	}

	public TerminalServiceLocator(URI url) {
		super(url);
	}

	@Override
	public Class<?> getType() {
		return ITerminalService.class;
	}
}
