package com.netifera.platform.net.http.internal.ui.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import com.netifera.platform.net.http.internal.ui.Activator;
import com.netifera.platform.net.http.internal.ui.inputbar.WebInputBar;

public class GoAction extends Action implements IWorkbenchAction {
	public final static String ID = "go-spider-action";

	private final WebInputBar inputBar;
	
	public GoAction(WebInputBar bar) {
		setId(ID);
		this.inputBar = bar;
		setEnabled(false);
		bar.setAction(this);
		setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(Activator.PLUGIN_ID, getImagePath()));
	}

	public void dispose() {		
	}
	
	public String getImagePath() {
		final String os = System.getProperty("osgi.os");
		if(os != null && os.equals("macosx")) {
			return "icons/go24.png";
		} else {
			return "icons/go.png";
		}
	}
	
	public void run() {
		inputBar.runAction();
	}
}
