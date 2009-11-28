package com.netifera.platform.ui.spaces.actions;

import org.eclipse.jface.action.Action;

import com.netifera.platform.api.model.ISpace;
import com.netifera.platform.ui.internal.spaces.Activator;

public class DeleteSpaceAction extends Action {
	final private ISpace space;

	public DeleteSpaceAction(ISpace space) {
		this.space = space;
		setId("deleteSpaceAction");
		setText("Delete");
		setImageDescriptor(Activator.getInstance().getImageCache().getDescriptor("icons/delete_hover.png"));
	}

	public void run() {
		deleteSpace(space);
	}

	private void deleteSpace(ISpace space) {
//		space.delete();
	}
}