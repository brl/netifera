package com.netifera.platform.host.filesystem.ui.probe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.action.IAction;

import com.netifera.platform.api.log.ILogManager;
import com.netifera.platform.api.log.ILogger;
import com.netifera.platform.api.model.IShadowEntity;
import com.netifera.platform.api.probe.IProbe;
import com.netifera.platform.api.probe.IProbeManagerService;
import com.netifera.platform.host.filesystem.spider.IFileSystemSpiderModule;
import com.netifera.platform.host.filesystem.tools.FileSystemHarvester;
import com.netifera.platform.model.ProbeEntity;
import com.netifera.platform.tools.options.IntegerOption;
import com.netifera.platform.tools.options.MultipleStringOption;
import com.netifera.platform.tools.options.StringOption;
import com.netifera.platform.ui.actions.ToolAction;
import com.netifera.platform.ui.api.actions.IEntityActionProvider;

public class EntityActionProvider implements IEntityActionProvider {

	private ILogger logger;
	private IProbeManagerService probeManager;
	final private List<IFileSystemSpiderModule> modules = new ArrayList<IFileSystemSpiderModule>();
	
	public List<IAction> getActions(IShadowEntity shadow) {
		List<IAction> actions = new ArrayList<IAction>();
		if(shadow instanceof ProbeEntity) {
			ProbeEntity probeEntity = (ProbeEntity) shadow;
			IProbe probe = probeManager.getProbeById(probeEntity.getProbeId());
			if(probe != null && probe.isConnected()) {
				actions.add(new OpenProbeFileSystemViewAction(logger, probe));
				
				ToolAction harvester = new ToolAction("Harvest File System", FileSystemHarvester.class.getName());
				harvester.addFixedOption(new StringOption("target", "Target", "Target File System", "file:///"));
				harvester.addOption(new MultipleStringOption("modules", "Modules", "Harvesting modules to activate during this havesting session", "Modules", getAvailableFileSystemSpiderModules()));
				harvester.addOption(new IntegerOption("maximumThreads", "Maximum threads", "Maximum number of threads", 5));
				harvester.addOption(new IntegerOption("bufferSize", "Buffer size", "Maximum bytes to fetch for each file", 1024*16));
				actions.add(harvester);
			}
		}
		
		return actions;
	}

	public List<IAction> getQuickActions(IShadowEntity shadow) {
		return Collections.emptyList();
	}

	protected void setLogManager(ILogManager logManager) {
		logger = logManager.getLogger("Probe Actions");
	}
	
	protected void unsetLogManager(ILogManager logManager) {
		
	}
	protected void setProbeManager(IProbeManagerService probeManager) {
		this.probeManager = probeManager;
	}
	
	protected void unsetProbeManager(IProbeManagerService probeManager) {
		
	}
	
	private String[] getAvailableFileSystemSpiderModules() {
		List<String> names = new ArrayList<String>();
		for (IFileSystemSpiderModule module: modules)
			names.add(module.getName());
		return names.toArray(new String[names.size()]);
	}
	
	protected void registerWebSpiderModule(IFileSystemSpiderModule module) {
		this.modules.add(module);
	}
	
	protected void unregisterWebSpiderModule(IFileSystemSpiderModule module) {
		this.modules.remove(module);
	}
}
