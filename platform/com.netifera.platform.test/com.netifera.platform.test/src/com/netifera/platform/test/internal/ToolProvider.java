package com.netifera.platform.test.internal;

import com.netifera.platform.api.tools.ITool;
import com.netifera.platform.api.tools.IToolProvider;
import com.netifera.platform.test.tools.FakeScanner;
import com.netifera.platform.test.tools.TestConnect;

public class ToolProvider implements IToolProvider {

	private final static String[] toolClassNames = { 
		FakeScanner.class.getName(),
		TestConnect.class.getName()
	};
	
	public ITool createToolInstance(String className) {
		if(className.equals(FakeScanner.class.getName())) {
			return new FakeScanner();
		} else if(className.equals(TestConnect.class.getName())) {
			return new TestConnect();
		}
		return null;
	}

	public String[] getProvidedToolClassNames() {
		return toolClassNames;
	}
}
