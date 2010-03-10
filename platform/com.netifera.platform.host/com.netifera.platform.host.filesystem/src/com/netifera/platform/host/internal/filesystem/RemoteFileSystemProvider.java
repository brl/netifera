package com.netifera.platform.host.internal.filesystem;

import java.net.URI;

import com.netifera.platform.api.log.ILogManager;
import com.netifera.platform.api.log.ILogger;
import com.netifera.platform.api.probe.IProbe;
import com.netifera.platform.host.filesystem.IFileSystem;
import com.netifera.platform.host.filesystem.probe.RemoteFileSystem;
import com.netifera.platform.services.IRemoteServiceProvider;

public class RemoteFileSystemProvider implements IRemoteServiceProvider {

	private ILogger logger;

	public IFileSystem create(URI url, IProbe probe) {
		return new RemoteFileSystem(url, probe, logger);
	}
	
	public Class<?> getType() {
		return RemoteFileSystem.class;
	}
	
	protected void setLogManager(ILogManager logManager) {
		logger = logManager.getLogger("File System");
	}
	
	protected void unsetLogManager(ILogManager logManager) {
		
	}
}
