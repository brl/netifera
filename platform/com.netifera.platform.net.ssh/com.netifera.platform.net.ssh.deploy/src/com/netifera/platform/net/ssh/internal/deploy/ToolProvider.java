package com.netifera.platform.net.ssh.internal.deploy;

import com.netifera.platform.api.tools.ITool;
import com.netifera.platform.api.tools.IToolProvider;
import com.netifera.platform.net.ssh.deploy.SSHProbeDeployer;

public class ToolProvider implements IToolProvider {

	private final static String[] toolClassNames = { 
		SSHProbeDeployer.class.getName()
	};
	
	public ITool createToolInstance(String className) {
		if(className.equals(SSHProbeDeployer.class.getName()))
			return new SSHProbeDeployer();
		return null;
	}

	public String[] getProvidedToolClassNames() {
		return toolClassNames;
	}
}
