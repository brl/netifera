package com.netifera.platform.net.cifs.internal.tools;

import com.netifera.platform.api.tools.ITool;
import com.netifera.platform.api.tools.IToolProvider;
import com.netifera.platform.net.cifs.tools.LMAuthBruteforcer;
import com.netifera.platform.net.cifs.tools.NTLMAuthBruteforcer;

public class ToolProvider implements IToolProvider {

	private final static String[] toolClassNames = { 
		LMAuthBruteforcer.class.getName(),
		NTLMAuthBruteforcer.class.getName()
	};
	
	public ITool createToolInstance(String className) {
		if(className.equals(LMAuthBruteforcer.class.getName()))
			return new LMAuthBruteforcer();
		if(className.equals(NTLMAuthBruteforcer.class.getName()))
			return new NTLMAuthBruteforcer();
		return null;
	}

	public String[] getProvidedToolClassNames() {
		return toolClassNames;
	}
}
