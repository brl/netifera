package com.netifera.platform.net.http.internal.ui.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import com.netifera.platform.api.model.ISpace;
import com.netifera.platform.net.http.internal.ui.Activator;
import com.netifera.platform.net.http.spider.daemon.IWebSpiderDaemon;

/**
 * An action for starting the web spider daemon.
 * 
 * @see com.netifera.platform.http.spider.daemon.IWebSpiderDaemon
 * 
 *
 */
public class StartSpiderAction extends Action {
	public final static String ID = "start-spider-action";

	private final WebSpiderActionManager manager;

	StartSpiderAction(WebSpiderActionManager manager) {
		setId(ID);
		this.manager = manager;
		setToolTipText("Start Web Spider");
		setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(Activator.PLUGIN_ID, "icons/start_16x16.png"));
	}

	public void run() {
		final ISpace space = Activator.getDefault().getCurrentSpace();
		if(space == null) {
			return;
		}
		final IWebSpiderDaemon daemon = Activator.getDefault().getWebSpiderDaemon();
		if(daemon == null) {
			manager.setFailed("No sniffing service found");
			return;
		}
		
/*		if(!hasInterfacesAvailable(daemon)) {
			manager.setState();
			return;
		}
*/		
		new Thread(new Runnable() {
			public void run() {
				daemon.start(space.getId());
				Display.getDefault().asyncExec(new Runnable() {
					public void run() {
						manager.setState();
					}
				});
			}
		}).start();
	}
}
