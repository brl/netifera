package com.netifera.platform.ui.world.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import com.netifera.platform.ui.internal.world.Activator;
import com.netifera.platform.ui.world.GlobeWorldView;

public class TogglePlaceNamesAction extends Action {
	
	final private GlobeWorldView view;
	
	public TogglePlaceNamesAction(GlobeWorldView view) {
		this.view = view;
		setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(Activator.PLUGIN_ID, "icons/placenames.png"));
		update();
	}
	
	public void run() {
		view.setPlaceNamesEnabled(!view.isPlaceNamesEnabled());
		update();
	}
	
	private void update() {
		if(view.isPlaceNamesEnabled()) {
			setChecked(true);
			setToolTipText("Hide Place Names");
		} else {
			setChecked(false);
			setToolTipText("Show Place Names");
		}
	}
}
