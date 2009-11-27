package com.netifera.platform.host.filesystem.tools.internal;

import com.netifera.platform.api.tools.ITool;
import com.netifera.platform.api.tools.IToolProvider;
import com.netifera.platform.host.filesystem.tools.FileSystemHarvester;
import com.netifera.platform.host.filesystem.tools.Netstat;

public class ToolProvider implements IToolProvider {
	private final static String[] toolClassNames = { 
		FileSystemHarvester.class.getName(),
		Netstat.class.getName()
	};
	
	public ITool createToolInstance(String className) {
		if(className.equals(FileSystemHarvester.class.getName())) {
			return new FileSystemHarvester();
		}
		if(className.equals(Netstat.class.getName())) {
			return new Netstat();
		}
		return null;
	}

	public String[] getProvidedToolClassNames() {
		return toolClassNames;
	}
}
