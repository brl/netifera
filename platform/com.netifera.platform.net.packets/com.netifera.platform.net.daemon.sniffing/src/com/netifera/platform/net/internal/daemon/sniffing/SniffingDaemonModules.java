package com.netifera.platform.net.internal.daemon.sniffing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.netifera.platform.api.dispatcher.IMessenger;
import com.netifera.platform.api.dispatcher.MessengerException;
import com.netifera.platform.api.log.ILogger;
import com.netifera.platform.api.probe.IProbeManagerService;
import com.netifera.platform.net.daemon.sniffing.model.ISniffingEntityFactory;
import com.netifera.platform.net.daemon.sniffing.model.NetworkInterfaceEntity;
import com.netifera.platform.net.daemon.sniffing.model.SniffingSessionEntity;
import com.netifera.platform.net.daemon.sniffing.module.ISniffingModule;
import com.netifera.platform.net.internal.daemon.remote.ModuleRecord;
import com.netifera.platform.net.internal.daemon.remote.RequestModuleInformation;
import com.netifera.platform.net.internal.daemon.remote.SetModuleEnableState;
import com.netifera.platform.net.internal.daemon.remote.SniffingModuleOutput;
import com.netifera.platform.net.internal.daemon.remote.StartSniffingDaemon;
import com.netifera.platform.net.internal.daemon.remote.StopSniffingDaemon;
import com.netifera.platform.net.pcap.ICaptureInterface;
import com.netifera.platform.net.sniffing.ISniffingEngineService;

public class SniffingDaemonModules {
	/*
	 * All sniffing modules which have been registered with the daemon.
	 */
	private final Set<ISniffingModule> modules;

	/*
	 * Sniffing modules which have been enabled with the enableModules()
	 * method.
	 */
	private final Set<EnabledSniffingModule> enabledModules;
	
	private final Map<String, ISniffingModule> moduleByName;
	
	private ISniffingEntityFactory entityFactory;
	private IProbeManagerService probeManager;
	private ISniffingEngineService sniffingEngine;
	private ILogger logger;
	private boolean isInitialized;
	
	private volatile boolean isRunning;
	private IMessenger openMessenger;
	
	private final ISniffingModuleOutput moduleOutput;
	private final SniffingDaemonInterfaces interfaces;
	

	public SniffingDaemonModules(SniffingDaemonInterfaces interfaces) {
		this.interfaces = interfaces;
		
		modules = new TreeSet<ISniffingModule>(new Comparator<ISniffingModule>() {

			public int compare(ISniffingModule module1,
					ISniffingModule module2) {
				return module1.getName().compareTo(module2.getName());
			}

		});

		enabledModules = new HashSet<EnabledSniffingModule>();
		moduleByName = new HashMap<String, ISniffingModule>();
		moduleOutput = createSniffingModuleOutput();
		
	}
	
	public ILogger getLogger() {
		return logger;
	}
	
	public ISniffingModuleOutput getModuleOutput() {
		return moduleOutput;
	}
	
	public void setModuleEnableState(IMessenger messenger, SetModuleEnableState msg) throws MessengerException {
		for(ModuleRecord module : msg.getModuleRecords()) {
			final ISniffingModule sniffingModule = getModuleByName(module.getName());
			if(sniffingModule == null) {
				logger.warning("No sniffing module found with name : " + module.getName());
			} else {
				if(module.isEnabled())
					enableModule(sniffingModule);
				else
					disableModule(sniffingModule);
			}
		}
		messenger.respondOk(msg);
	}
	
	public void requestModuleInformation(IMessenger messenger, RequestModuleInformation msg) throws MessengerException {
		final List <ModuleRecord> result = new ArrayList<ModuleRecord>();
		for(ISniffingModule mod : getModules()) {
			result.add(new ModuleRecord(mod.getName(), isEnabled(mod)));
		}
		messenger.emitMessage(msg.createResponse(result));		
	}
	
	
	public void startSniffingDaemon(IMessenger messenger, StartSniffingDaemon msg) throws MessengerException {
		verifyInitialized();
		final long realmId = probeManager.getLocalProbe().getEntity().getId();
		final SniffingSessionEntity session = entityFactory.createSniffingSession(realmId, msg.getSpaceId());
		start(messenger, sniffingEngine, interfaces.getEnabledInterfaces(), msg.getSpaceId(), session.getId());
		messenger.respondOk(msg);
	}
	
	public void stopSniffingDaemon(IMessenger messenger, StopSniffingDaemon msg) throws MessengerException {
		verifyInitialized();
		stop();
		messenger.respondOk(msg);
	}
	
	public void setServices(ILogger logger, ISniffingEntityFactory factory, IProbeManagerService probeManager, ISniffingEngineService sniffingEngine) {
		if(logger == null || factory == null || probeManager == null || sniffingEngine == null) {
			throw new IllegalArgumentException();
		}
		
		this.logger = logger;
		this.entityFactory = factory;
		this.probeManager = probeManager;
		this.sniffingEngine = sniffingEngine;
		this.isInitialized = true;
	}
	
	private void verifyInitialized() {
		if(!isInitialized) {
			throw new IllegalStateException("Sniffing Daemon Module subsystem is not initialized");
		}
	}
	
	
	
	public boolean isRunning() {
		return isRunning;
	}
	
	Set<ISniffingModule> getModules() {
		return Collections.unmodifiableSet(modules);
	}
	
	Set<EnabledSniffingModule> getEnabledModules() {
		return Collections.unmodifiableSet(enabledModules);
	}
	
	private void start(IMessenger messenger, ISniffingEngineService sniffingEngine, Collection<ICaptureInterface> interfaces,
			long spaceId, long realmId) {
		if(isRunning)
			return;
		isRunning = true;
		openMessenger = messenger;
		
		final Set<SniffingDaemonInterface> ifs = new HashSet<SniffingDaemonInterface>();
		
		for(ICaptureInterface iface : interfaces) {
			final NetworkInterfaceEntity interfaceEntity  = entityFactory.createNetworkInterface(realmId, spaceId, iface.getName());
			ifs.add(new SniffingDaemonInterface(iface, interfaceEntity.getId()));
		}
		
		synchronized (enabledModules) {
			for(EnabledSniffingModule module : enabledModules) {
				module.start(sniffingEngine, ifs, spaceId);
			}
		}
	}
	
	void stop() {
		if(!isRunning) {
			return;
		}
		for(EnabledSniffingModule module : enabledModules) {
			module.stop();
		}
		isRunning = false;
	}
	
	ISniffingModule getModuleByName(String name) {
		return moduleByName.get(name);
	}
	
	public void addModule(ISniffingModule module) {
		synchronized (modules) {
			modules.add(module);
			moduleByName.put(module.getName(), module);
		}
		
		enableModule(module);
	}
	
	public void removeModule(ISniffingModule module) {
		disableModule(module);
		synchronized (module) {
			modules.remove(module);
			moduleByName.remove(module.getName());
		}
		
	}
	
	void enableModule(ISniffingModule module) {
		if(isRunning) {
			throw new IllegalStateException("Cannot enable modules while daemon is running");
		}
		
		if(!getModules().contains(module)) {
			throw new IllegalArgumentException("Unknown module passed to enableModule() " + module);
		}
		
		synchronized (enabledModules) {
			if(findEnabledModule(module) != null)
				return;
			enabledModules.add(new EnabledSniffingModule(module, this));

		}		
	}

	void disableModule(ISniffingModule module) {
		if(isRunning) {
			throw new IllegalStateException("Cannot disable modules while daemon is running");

		}
		if(!getModules().contains(module)) {
			throw new IllegalArgumentException("Unknown module passed to disableModule() " + module);
		}
		
		synchronized(enabledModules) {
			final EnabledSniffingModule esm = findEnabledModule(module);
			if(esm != null) 
				enabledModules.remove(esm);	
		}
	}
	
	boolean isEnabled(ISniffingModule module) {
		synchronized (enabledModules) {
			return findEnabledModule(module) != null;
		}
	}
	/*
	 * Call with lock on enabledModules
	 */
	private EnabledSniffingModule findEnabledModule(ISniffingModule module) {
		for(EnabledSniffingModule m : enabledModules) {
			if(m.getModule().equals(module))
				return m;
		}
		return null;
	}
	
	private ISniffingModuleOutput createSniffingModuleOutput() {
		return new ISniffingModuleOutput() {
			public void printOutput(String output) {
				printModuleOutput(output);
			}
		};
	}
	
	private void printModuleOutput(String message) {
		if(openMessenger == null)
			return;
		if(!message.endsWith("\n")) {
			message = message.concat("\n");
		}
		
		try {
			openMessenger.emitMessage(new SniffingModuleOutput(message));
		} catch(MessengerException e) {
			openMessenger = null;
		}
	}
}
