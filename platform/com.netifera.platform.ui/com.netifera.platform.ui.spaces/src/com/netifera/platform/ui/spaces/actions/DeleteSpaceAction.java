package com.netifera.platform.ui.spaces.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

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
		if (space.isActive()) {
			MessageDialog.openError(Display.getCurrent().getActiveShell(), "Can't delete", "The space '"+space.getName()+"' has tasks running. Stop the tasks before deleting.");
			return;
		}
		if (!space.isIsolated() || MessageDialog.openConfirm(Display.getCurrent().getActiveShell(), "Are you sure?", "Delete isolated space '"+space.getName()+"' and recursively all entities under its realm?")) {
			space.delete();
		}
	}
}
