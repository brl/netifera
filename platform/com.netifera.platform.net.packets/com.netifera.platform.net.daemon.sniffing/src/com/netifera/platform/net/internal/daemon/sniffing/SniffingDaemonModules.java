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
import com.netifera.platform.net.daemon.sniffing.extend.AbstractSniffingDaemon;
import com.netifera.platform.net.daemon.sniffing.extend.IModuleExecutionProvider;
import com.netifera.platform.net.daemon.sniffing.model.NetworkInterfaceEntity;
import com.netifera.platform.net.daemon.sniffing.module.ISniffingModule;
import com.netifera.platform.net.internal.daemon.remote.ModuleRecord;
import com.netifera.platform.net.internal.daemon.remote.RequestModuleInformation;
import com.netifera.platform.net.internal.daemon.remote.SetModuleEnableState;
import com.netifera.platform.net.internal.daemon.remote.SniffingModuleOutput;
import com.netifera.platform.net.pcap.ICaptureInterface;

public class SniffingDaemonModules implements IModuleExecutionProvider {
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
	
	
	private volatile boolean isRunning;
	
	private final ISniffingModuleOutput moduleOutput;
	
	private final AbstractSniffingDaemon sniffingDaemon;

	public SniffingDaemonModules(AbstractSniffingDaemon sniffingDaemon) {
		this.sniffingDaemon = sniffingDaemon;
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
		return sniffingDaemon.getLogger();
	}
	
	public ISniffingModuleOutput getModuleOutput() {
		return moduleOutput;
	}
	
	public void setModuleEnableState(IMessenger messenger, SetModuleEnableState msg) throws MessengerException {
		for(ModuleRecord module : msg.getModuleRecords()) {
			final ISniffingModule sniffingModule = getModuleByName(module.getName());
			if(sniffingModule == null) {
				sniffingDaemon.getLogger().warning("No sniffing module found with name : " + module.getName());
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
		messenger.emitMessage(msg.createResponse(sniffingDaemon.getMessagePrefix(), result));		
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
	
	public void startModules(Collection<ICaptureInterface> interfaces,
			long spaceId, long realmId, boolean createInterfaceEntities) {
		if(isRunning)
			return;
		isRunning = true;
		
		final Set<SniffingDaemonInterface> ifs = new HashSet<SniffingDaemonInterface>();
		
		for(ICaptureInterface iface : interfaces) {
			if(createInterfaceEntities) {
				final NetworkInterfaceEntity interfaceEntity  = sniffingDaemon.createNetworkInterfaceEntity(realmId, spaceId, iface.getName());
				ifs.add(new SniffingDaemonInterface(iface, interfaceEntity.getId()));
			} else {
				ifs.add(new SniffingDaemonInterface(iface, realmId));
			}
		}
		
		synchronized (enabledModules) {
			for(EnabledSniffingModule module : enabledModules) {
				module.start(sniffingDaemon.getSniffingEngine(), ifs, spaceId);
			}
		}
	}
	
	public void stopModules() {
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
		if(sniffingDaemon.getActiveMessenger() == null)
			return;
		if(!message.endsWith("\n")) {
			message = message.concat("\n");
		}
		
		try {
			sniffingDaemon.getActiveMessenger().emitMessage(new SniffingModuleOutput(sniffingDaemon.getMessagePrefix(), message));
		} catch(MessengerException e) {
			sniffingDaemon.setActiveMessengerClosed();
		}
	}
}
