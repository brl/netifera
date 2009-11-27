package com.netifera.platform.host.internal.processes;

import java.net.URI;

import com.netifera.platform.api.log.ILogManager;
import com.netifera.platform.api.log.ILogger;
import com.netifera.platform.host.processes.IProcessService;
import com.netifera.platform.host.processes.linux.LinuxProcessService;
import com.netifera.platform.services.IServiceProvider;

public class LocalProcessServiceProvider implements IServiceProvider {
	
	private ILogger logger;

	public Class<?> getType() {
		return IProcessService.class;
	}

	public Object create(URI url) {
		if (url.getScheme().equals("local"))
			return new LinuxProcessService(logger);
		return null;
	}
	
	protected void setLogManager(ILogManager logManager) {
		logger = logManager.getLogger("Process Service");
	}
	
	protected void unsetLogManager(ILogManager logManager) {
	}
}
