package com.netifera.platform.net.internal.tools.portscanning;

import com.netifera.platform.api.tools.ITool;
import com.netifera.platform.api.tools.IToolProvider;
import com.netifera.platform.net.tools.basic.AddHost;
import com.netifera.platform.net.tools.basic.AddNetblocks;
import com.netifera.platform.net.tools.basic.AddService;
import com.netifera.platform.net.tools.portscanning.TCPConnectScanner;
import com.netifera.platform.net.tools.portscanning.UDPScanner;

public class ToolProvider implements IToolProvider {

	private final static String[] toolClassNames = { 
		AddHost.class.getName(),
		AddNetblocks.class.getName(),
		AddService.class.getName(),
		TCPConnectScanner.class.getName(),
		UDPScanner.class.getName()
	};
	
	public ITool createToolInstance(String className) {
		if(className.equals(AddHost.class.getName())) {
			return new AddHost();
		} else if(className.equals(AddNetblocks.class.getName())) {
			return new AddNetblocks();
		} else if(className.equals(AddService.class.getName())) {
			return new AddService();
		} else if(className.equals(TCPConnectScanner.class.getName())) {
			return new TCPConnectScanner();
		} else if(className.equals(UDPScanner.class.getName())) {
			return new UDPScanner();
		}
		return null;
	}

	public String[] getProvidedToolClassNames() {
		return toolClassNames;
	}
}
