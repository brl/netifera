package com.netifera.platform.net.http.internal.ui;

import org.eclipse.ui.IStartup;

public class Startup implements IStartup {

	public void earlyStartup() {
		/*
		 * Workbench is initialized when this is called.
		 */
		Activator.getDefault().initialize();
	}

}
