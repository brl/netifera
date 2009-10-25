package com.netifera.platform.services.internal;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import com.netifera.platform.api.log.ILogManager;
import com.netifera.platform.api.log.ILogger;
import com.netifera.platform.api.probe.IProbe;
import com.netifera.platform.api.probe.IProbeManagerService;
import com.netifera.platform.services.IRemoteServiceProvider;
import com.netifera.platform.services.IServiceFactory;
import com.netifera.platform.services.IServiceProvider;

public class ServiceFactory implements IServiceFactory {

	private List<IServiceProvider> providers = new ArrayList<IServiceProvider>();
	private List<IRemoteServiceProvider> remoteProviders = new ArrayList<IRemoteServiceProvider>();
	
	private ILogger logger;
	private IProbeManagerService probeManager;
	
	public Object create(Class<?> serviceType, URI url) {
		for (IServiceProvider provider: providers) {
			if (serviceType.isAssignableFrom(provider.getType())) {
				Object service = provider.create(url);
				if (service != null)
					return service;
			}
		}
		logger.info("Service Not Found: "+serviceType.getName()+" "+url);
		return null;
	}

	public Object create(Class<?> serviceType, URI url, IProbe probe) {
		if (probe.isLocalProbe())
			return create(serviceType, url);
		for (IRemoteServiceProvider provider: remoteProviders) {
			if (serviceType.isAssignableFrom(provider.getType())) {
				Object service = provider.create(url, probe);
				if (service != null)
					return service;
			}
		}
		logger.info("Service Not Found: "+serviceType.getName()+" "+url+" "+probe);
		return null;
	}

	public Object create(Class<?> serviceType, URI url, long probeId) {
		return create(serviceType, url, probeManager.getProbeById(probeId));
	}

	protected void registerProvider(IServiceProvider provider) {
		logger.info("Register Service Provider: "+provider.getClass().getName());
		providers.add(provider);
	}

	protected void unregisterProvider(IServiceProvider provider) {
		providers.remove(provider);
	}

	protected void registerRemoteProvider(IRemoteServiceProvider provider) {
		logger.info("Register Remote Service Provider: "+provider.getClass().getName());
		remoteProviders.add(provider);
	}

	protected void unregisterRemoteProvider(IRemoteServiceProvider provider) {
		remoteProviders.remove(provider);
	}

	protected void setLogManager(ILogManager logManager) {
		logger = logManager.getLogger("Service Factory");
	}
	
	protected void unsetLogManager(ILogManager logManager) {
	}

	protected void setProbeManager(IProbeManagerService probeManager) {
		this.probeManager = probeManager;
	}
	
	protected void unsetProbeManager(IProbeManagerService probeManager) {
	}
}
