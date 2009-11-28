package com.netifera.platform.ui.spaces.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.InputDialog;

import com.netifera.platform.api.model.ISpace;
import com.netifera.platform.ui.internal.spaces.Activator;

public class RenameSpaceAction extends Action {
	final private ISpace space;

	public RenameSpaceAction(ISpace space) {
		this.space = space;
		setId("renameSpaceAction");
		setText("Rename");
		setImageDescriptor(Activator.getInstance().getImageCache().getDescriptor("icons/rename.png"));
	}

	public void run() {
		renameSpace(space);
	}

	/**
	 * Ask the user for a new name
	 * @param name current space name
	 * @return new name string or current name if the user provided one is invalid
	 */
	private String askName(final String name) {
		final InputDialog dialog = new InputDialog(null,"Rename space", "Type a new name for the space", name, null);
		dialog.create();

		if( dialog.open() == 0) {
			final String newName = dialog.getValue();
			if(newName != null && newName.length() > 0) {
				return newName;
			}
		}
		return name;
	}

	private void renameSpace(ISpace space) {
		final String name = space.getName();    
		final String newName = askName(name);

		if(!name.equals(newName)) {  
			space.setName(newName);
		}
	}
}