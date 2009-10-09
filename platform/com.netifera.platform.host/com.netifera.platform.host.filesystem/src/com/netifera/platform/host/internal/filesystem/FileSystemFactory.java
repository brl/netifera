package com.netifera.platform.host.internal.filesystem;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.netifera.platform.api.log.ILogManager;
import com.netifera.platform.api.log.ILogger;
import com.netifera.platform.api.probe.IProbe;
import com.netifera.platform.host.filesystem.IFileSystem;
import com.netifera.platform.host.filesystem.IFileSystemFactory;
import com.netifera.platform.host.filesystem.IFileSystemProvider;
import com.netifera.platform.host.filesystem.probe.RemoteFileSystem;

public class FileSystemFactory implements IFileSystemFactory {

	private List<IFileSystemProvider> providers = new ArrayList<IFileSystemProvider>();
	private ILogger logger;
	private Map<IProbe, RemoteFileSystem> probeMap = new HashMap<IProbe, RemoteFileSystem>();

	@Deprecated
	public IFileSystem createForProbe(IProbe probe) {
		if(probeMap.containsKey(probe)) {
			return probeMap.get(probe);
		}
		final RemoteFileSystem rfs = new RemoteFileSystem(probe, logger);
		probeMap.put(probe, rfs);
		return rfs;
	}

	public IFileSystem create(URI url) {
		for (IFileSystemProvider provider: providers) {
			if (provider.getScheme().equals(url.getScheme()))
				return provider.create(url);
		}
		return null;
	}

	public IFileSystem create(URI url, IProbe probe) {
		// TODO Auto-generated method stub
		return null;
	}

	protected void registerProvider(IFileSystemProvider provider) {
		providers.add(provider);
	}

	protected void unregisterProvider(IFileSystemProvider provider) {
		providers.remove(provider);
	}

	protected void setLogManager(ILogManager logManager) {
		logger = logManager.getLogger("File System");
	}
	
	protected void unsetLogManager(ILogManager logManager) {
		
	}

}
