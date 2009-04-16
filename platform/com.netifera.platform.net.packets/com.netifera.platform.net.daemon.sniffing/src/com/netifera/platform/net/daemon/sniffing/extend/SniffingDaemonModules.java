package com.netifera.platform.net.daemon.sniffing.extend;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.netifera.platform.api.dispatcher.IMessenger;
import com.netifera.platform.api.dispatcher.MessengerException;
import com.netifera.platform.api.log.ILogger;
import com.netifera.platform.net.daemon.sniffing.ISniffingModule;
import com.netifera.platform.net.daemon.sniffing.model.ISniffingEntityFactory;
import com.netifera.platform.net.daemon.sniffing.model.NetworkInterfaceEntity;
import com.netifera.platform.net.internal.daemon.probe.SniffingModuleOutput;
import com.netifera.platform.net.internal.daemon.sniffing.EnabledSniffingModule;
import com.netifera.platform.net.internal.daemon.sniffing.ISniffingModuleOutput;
import com.netifera.platform.net.internal.daemon.sniffing.SniffingDaemonInterface;
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
	
	private volatile boolean isRunning;
	private IMessenger openMessenger;
	private ILogger logger;
	
	private final ISniffingModuleOutput moduleOutput;
	
	public SniffingDaemonModules() {
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
	
	public void setLogger(ILogger logger) {
		this.logger = logger;
	}
	public ILogger getLogger() {
		return logger;
	}
	
	public void setEntityFactory(ISniffingEntityFactory factory) {
		this.entityFactory = factory;
	}
	
	public ISniffingModuleOutput getModuleOutput() {
		return moduleOutput;
	}
	
	boolean isRunning() {
		return isRunning;
	}
	
	Set<ISniffingModule> getModules() {
		return Collections.unmodifiableSet(modules);
	}
	
	void start(IMessenger messenger, ISniffingEngineService sniffingEngine, Collection<ICaptureInterface> interfaces,
			long spaceId, long realmId) {
		System.out.println("starting... with realmId = " + realmId);
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
	
	void addModule(ISniffingModule module) {
		System.out.println("adding " + module);
		synchronized (modules) {
			modules.add(module);
			moduleByName.put(module.getName(), module);
		}
		
		enableModule(module);
	}
	
	void removeModule(ISniffingModule module) {
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
