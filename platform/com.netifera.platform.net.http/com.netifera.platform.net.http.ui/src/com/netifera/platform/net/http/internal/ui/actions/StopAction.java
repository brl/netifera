package com.netifera.platform.net.http.internal.ui.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import com.netifera.platform.net.http.internal.ui.Activator;
import com.netifera.platform.net.http.spider.daemon.IWebSpiderDaemon;

public class StopAction extends Action {
	public final static String ID = "stop-spider-action";

	private final WebSpiderActionManager manager;
	
	public StopAction(WebSpiderActionManager manager) {
		setId(ID);
		this.manager = manager;
		setToolTipText("Stop Web Spider");
		setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(Activator.PLUGIN_ID, "icons/stop_16x16.png"));
	}
	
	public void run() {
		final IWebSpiderDaemon daemon = Activator.getDefault().getWebSpiderDaemon();
		if(daemon == null) {
			manager.setFailed("No web spider service found");
			return;
		}

		new Thread(new Runnable() {
			public void run() {
				daemon.stop();
				Display.getDefault().asyncExec(new Runnable() {
					public void run() {
						manager.setState();
					}
				});
			}
		}).start();
	}
}
