package com.netifera.platform.net.ssh.internal.tools;

import com.netifera.platform.api.tools.ITool;
import com.netifera.platform.api.tools.IToolProvider;
import com.netifera.platform.net.ssh.tools.SSHAuthBruteforcer;
import com.netifera.platform.net.ssh.tools.SSHProbeDeployer;

public class ToolProvider implements IToolProvider {

	private final static String[] toolClassNames = {
		SSHAuthBruteforcer.class.getName(),
		SSHProbeDeployer.class.getName()
	};
	
	public ITool createToolInstance(String className) {
		if(className.equals(SSHAuthBruteforcer.class.getName()))
			return new SSHAuthBruteforcer();
		if(className.equals(SSHProbeDeployer.class.getName()))
			return new SSHProbeDeployer();
		return null;
	}

	public String[] getProvidedToolClassNames() {
		return toolClassNames;
	}
}
