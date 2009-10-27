package com.netifera.platform.host.internal.processes;

import java.net.URI;

import com.netifera.platform.api.log.ILogManager;
import com.netifera.platform.api.log.ILogger;
import com.netifera.platform.api.probe.IProbe;
import com.netifera.platform.host.processes.probe.RemoteProcessService;
import com.netifera.platform.services.IRemoteServiceProvider;

public class RemoteProcessServiceProvider implements IRemoteServiceProvider {

	private ILogger logger;

	public Object create(URI url, IProbe probe) {
		return new RemoteProcessService(probe, logger);//XXX ignoring url
	}

	public Class<?> getType() {
		return RemoteProcessService.class;
	}
	
	protected void setLogManager(ILogManager logManager) {
		logger = logManager.getLogger("Process Service");
	}
	
	protected void unsetLogManager(ILogManager logManager) {
	}
}
