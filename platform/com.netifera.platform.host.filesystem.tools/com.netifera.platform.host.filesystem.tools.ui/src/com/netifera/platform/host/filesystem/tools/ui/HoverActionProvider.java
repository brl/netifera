package com.netifera.platform.host.filesystem.tools.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.action.IAction;

import com.netifera.platform.api.model.IShadowEntity;
import com.netifera.platform.host.filesystem.FileSystemServiceLocator;
import com.netifera.platform.host.filesystem.spider.IFileSystemSpiderModule;
import com.netifera.platform.host.filesystem.tools.FileSystemHarvester;
import com.netifera.platform.host.filesystem.tools.Netstat;
import com.netifera.platform.net.model.HostEntity;
import com.netifera.platform.tools.options.GenericOption;
import com.netifera.platform.tools.options.IntegerOption;
import com.netifera.platform.tools.options.MultipleStringOption;
import com.netifera.platform.tools.options.StringOption;
import com.netifera.platform.ui.actions.ToolAction;
import com.netifera.platform.ui.api.hover.IHoverActionProvider;
import com.netifera.platform.util.addresses.inet.InternetAddress;

public class HoverActionProvider implements IHoverActionProvider {

	final private List<IFileSystemSpiderModule> modules = new ArrayList<IFileSystemSpiderModule>();
	
	public List<IAction> getActions(Object o) {
		if (!(o instanceof IShadowEntity)) return Collections.emptyList();
		IShadowEntity shadow = (IShadowEntity) o;
		
		List<IAction> answer = new ArrayList<IAction>();
		
		FileSystemServiceLocator fileSystemLocator = (FileSystemServiceLocator) shadow.getAdapter(FileSystemServiceLocator.class);
		if (fileSystemLocator != null /*&& fileSystemLocator.getHost() != null*/) {
			ToolAction harvester = new ToolAction("Harvest File System", FileSystemHarvester.class.getName());
			harvester.addFixedOption(new StringOption("target", "Target", "Target File System", fileSystemLocator.getURL().toASCIIString()));
			harvester.addFixedOption(new GenericOption(InternetAddress.class, "host", "Host", "Host", fileSystemLocator.getHost() != null ? (InternetAddress)((HostEntity)fileSystemLocator.getHost()).getDefaultAddress().toNetworkAddress() : InternetAddress.fromString("127.0.0.1")));
			harvester.addOption(new MultipleStringOption("modules", "Modules", "Harvesting modules to activate during this havesting session", "Modules", getAvailableFileSystemSpiderModules()));
			harvester.addOption(new IntegerOption("maximumThreads", "Maximum threads", "Maximum number of threads", 5));
			harvester.addOption(new IntegerOption("bufferSize", "Buffer size", "Maximum bytes to fetch for each file", 1024*16));
			answer.add(harvester);
			
//			if (fileSystemLocator.getHost() != null && ((HostEntity)fileSystemLocator.getHost()).getPlatform().matches(".*linux.*") {
				ToolAction netstat = new ToolAction("Netstat", Netstat.class.getName());
				netstat.addFixedOption(new StringOption("target", "Target", "Target File System", fileSystemLocator.getURL().toASCIIString()));
				netstat.addFixedOption(new GenericOption(InternetAddress.class, "host", "Host", "Host", fileSystemLocator.getHost() != null ? (InternetAddress)((HostEntity)fileSystemLocator.getHost()).getDefaultAddress().toNetworkAddress() : InternetAddress.fromString("127.0.0.1")));
				answer.add(netstat);
//			}
		}
		
		return answer;
	}

	public List<IAction> getQuickActions(Object o) {
		return Collections.emptyList();
	}
	
	private String[] getAvailableFileSystemSpiderModules() {
		List<String> names = new ArrayList<String>();
		for (IFileSystemSpiderModule module: modules)
			names.add(module.getName());
		return names.toArray(new String[names.size()]);
	}
	
	protected void registerFileSystemSpiderModule(IFileSystemSpiderModule module) {
		this.modules.add(module);
	}
	
	protected void unregisterFileSystemSpiderModule(IFileSystemSpiderModule module) {
		this.modules.remove(module);
	}
}
