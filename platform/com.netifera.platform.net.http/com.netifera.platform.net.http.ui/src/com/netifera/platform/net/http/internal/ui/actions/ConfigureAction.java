package com.netifera.platform.net.http.internal.ui.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import com.netifera.platform.net.http.internal.ui.Activator;

public class ConfigureAction extends Action {
	public final static String ID = "spider-configure-action";

	private final WebSpiderActionManager manager;

	ConfigureAction(WebSpiderActionManager manager) {
		setId(ID);
		setToolTipText("Configure Web Spider");
		setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(
				Activator.PLUGIN_ID, "icons/configure.png"));
	
		this.manager = manager;
	}
	
	public void run() {
		ConfigPanel panel = new ConfigPanel(PlatformUI.getWorkbench().getDisplay().getActiveShell(), manager);
		panel.open();
	}
}
