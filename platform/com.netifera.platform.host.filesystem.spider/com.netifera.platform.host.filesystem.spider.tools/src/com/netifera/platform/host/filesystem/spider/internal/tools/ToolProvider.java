package com.netifera.platform.host.filesystem.spider.internal.tools;

import com.netifera.platform.api.tools.ITool;
import com.netifera.platform.api.tools.IToolProvider;
import com.netifera.platform.host.filesystem.spider.tools.FileSystemHarvester;

public class ToolProvider implements IToolProvider {
	private final static String[] toolClassNames = { 
		FileSystemHarvester.class.getName()
	};
	
	public ITool createToolInstance(String className) {
		if(className.equals(FileSystemHarvester.class.getName())) {
			return new FileSystemHarvester();
		}
		return null;
	}

	public String[] getProvidedToolClassNames() {
		return toolClassNames;
	}
}
