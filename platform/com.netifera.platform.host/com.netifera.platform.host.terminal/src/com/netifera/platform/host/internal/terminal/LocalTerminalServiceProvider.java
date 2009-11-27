package com.netifera.platform.host.internal.terminal;

import java.net.URI;

import com.netifera.platform.api.log.ILogManager;
import com.netifera.platform.api.log.ILogger;
import com.netifera.platform.api.system.ISystemService;
import com.netifera.platform.host.terminal.LocalTerminalService;
import com.netifera.platform.services.IServiceProvider;

public class LocalTerminalServiceProvider implements IServiceProvider {
	
	private ILogger logger;
	private ISystemService system;

	public Class<?> getType() {
		return LocalTerminalService.class;
	}

	public Object create(URI url) {
		if (url.getScheme().equals("local"))
			return new LocalTerminalService(logger, system);
		return null;
	}
	
	protected void setLogManager(ILogManager logManager) {
		logger = logManager.getLogger("Terminal Service");
	}
	
	protected void unsetLogManager(ILogManager logManager) {
	}
	
	protected void setSystemService(ISystemService system) {
		this.system = system;
	}
	
	protected void unsetSystemService(ISystemService system) {
	}
}
