package com.netifera.platform.ui.spaces.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

public class NewIsolatedSpaceDelegate implements IWorkbenchWindowActionDelegate {

	private SpaceCreator creator;
//	private IProbe selectedProbe;

	public void init(IWorkbenchWindow window) {
		creator = new SpaceCreator(window);
	}

	public void run(IAction action) {
/*		try {
			if (selectedProbe != null) {
				creator.openNewSpace(null, selectedProbe, true);
			} else {
				creator.openNewSpace(true);
			}
		} catch (IllegalArgumentException e) {
			// it means we tried to open an isolated Space on a remote probe
		}
*/
		creator.openNewIsolatedSpace();
	}

	public void selectionChanged(IAction action, ISelection selection) {
/*		if (selection instanceof IStructuredSelection) {
			Object element = ((IStructuredSelection)selection).getFirstElement();
			if (element instanceof IProbe) {
				selectedProbe = (IProbe) element;
			} else if (element instanceof ProbeEntity) {
				selectedProbe = Activator.getInstance().getProbeManager().getProbeById(((ProbeEntity)element).getProbeId());
			} else {
				selectedProbe = null;
			}
		} else {
			selectedProbe = null;
		}
*/	}
	
	public void dispose() {
	}
}
