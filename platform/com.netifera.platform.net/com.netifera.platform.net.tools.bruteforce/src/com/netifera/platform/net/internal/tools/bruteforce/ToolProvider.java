package com.netifera.platform.net.internal.tools.bruteforce;

import com.netifera.platform.api.tools.ITool;
import com.netifera.platform.api.tools.IToolProvider;
import com.netifera.platform.net.tools.bruteforce.FTPAuthBruteforcer;
import com.netifera.platform.net.tools.bruteforce.IMAPAuthBruteforcer;
import com.netifera.platform.net.tools.bruteforce.MSSQLAuthBruteforcer;
import com.netifera.platform.net.tools.bruteforce.POP3AuthBruteforcer;

public class ToolProvider implements IToolProvider {

	private final static String[] toolClassNames = { 
		FTPAuthBruteforcer.class.getName(),
		POP3AuthBruteforcer.class.getName(),
		IMAPAuthBruteforcer.class.getName(),
		MSSQLAuthBruteforcer.class.getName()
	};
	
	public ITool createToolInstance(String className) {
		if(className.equals(FTPAuthBruteforcer.class.getName()))
			return new FTPAuthBruteforcer();
		if(className.equals(POP3AuthBruteforcer.class.getName()))
			return new POP3AuthBruteforcer();
		if(className.equals(IMAPAuthBruteforcer.class.getName()))
			return new IMAPAuthBruteforcer();
		if(className.equals(MSSQLAuthBruteforcer.class.getName()))
			return new MSSQLAuthBruteforcer();
		return null;
	}

	public String[] getProvidedToolClassNames() {
		return toolClassNames;
	}
}
