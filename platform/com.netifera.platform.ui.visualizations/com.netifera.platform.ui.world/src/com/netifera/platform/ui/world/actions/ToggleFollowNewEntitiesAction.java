package com.netifera.platform.ui.world.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import com.netifera.platform.ui.internal.world.Activator;
import com.netifera.platform.ui.world.GlobeWorldView;

public class ToggleFollowNewEntitiesAction extends Action {
	
	final private GlobeWorldView view;
	
	public ToggleFollowNewEntitiesAction(GlobeWorldView view) {
		this.view = view;
		setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(Activator.PLUGIN_ID, "icons/follow_new.png"));
		update();
	}
	
	public void run() {
		view.setFollowNewEnabled(!view.isFollowNewEnabled());
		update();
	}
	
	private void update() {
		if(view.isFollowNewEnabled()) {
			setChecked(true);
			setToolTipText("Don't Fly To New Entities");
		} else {
			setChecked(false);
			setToolTipText("Fly To New Entities");
		}
	}
}
