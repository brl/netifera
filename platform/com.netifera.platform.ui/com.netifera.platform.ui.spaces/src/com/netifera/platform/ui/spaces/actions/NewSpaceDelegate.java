package com.netifera.platform.ui.spaces.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.netifera.platform.api.probe.IProbe;
import com.netifera.platform.model.ProbeEntity;
import com.netifera.platform.ui.internal.spaces.Activator;

public class NewSpaceDelegate implements IWorkbenchWindowActionDelegate {

	private SpaceCreator creator;
	private IProbe selectedProbe;
	
	public void init(IWorkbenchWindow window) {
		creator = new SpaceCreator(window);
	}

	public void run(IAction action) {
		if (selectedProbe != null) {
			creator.openNewSpace(null, selectedProbe, false);
		} else {
			creator.openNewSpace(false);
		}
	}

	public void selectionChanged(IAction action, ISelection selection) {
		if (selection instanceof IStructuredSelection) {
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
	}

	public void dispose() {
	}
}
