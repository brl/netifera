package com.netifera.platform.net.ssh.internal.terminal;

import java.net.URI;

import com.netifera.platform.host.terminal.ITerminalService;
import com.netifera.platform.services.IServiceProvider;

public class SSHTerminalProvider implements IServiceProvider {

	public ITerminalService create(URI url) {
		if (url.getScheme().equals("ssh"))
			return new SSHTerminalManager(url);
		return null;
	}
	
	public Class<?> getType() {
		return SSHTerminalManager.class;
	}
}
