package com.netifera.platform.ui.navigator;

import org.eclipse.jface.action.Action;

import com.netifera.platform.ui.internal.navigator.Activator;

public class ToggleLinkWithEditorAction extends Action {
	
	final private NavigatorView view;
	
	public ToggleLinkWithEditorAction(NavigatorView view) {
		this.view = view;
		setImageDescriptor(Activator.getInstance().getImageCache().getDescriptor("icons/link.png"));
		update();
	}
	
	public void run() {
		view.setLinkWithEditor(!view.isLinkWithEditor());
		update();
	}
	
	private void update() {
		setChecked(view.isLinkWithEditor());
	}
}
