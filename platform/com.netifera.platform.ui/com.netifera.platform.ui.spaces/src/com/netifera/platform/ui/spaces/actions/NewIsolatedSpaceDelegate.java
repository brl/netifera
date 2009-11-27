package com.netifera.platform.ui.spaces.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

public class NewIsolatedSpaceDelegate implements IWorkbenchWindowActionDelegate {

	private SpaceCreator creator;

	public void init(IWorkbenchWindow window) {
		creator = new SpaceCreator(window);
	}

	public void run(IAction action) {
		try {
			creator.openNewSpace(true);
		} catch (IllegalArgumentException e) {
			// it means we tried to open an isolated Space on a remote probe
		}
	}

	public void selectionChanged(IAction action, ISelection selection) {
	}
	
	public void dispose() {
	}
}
