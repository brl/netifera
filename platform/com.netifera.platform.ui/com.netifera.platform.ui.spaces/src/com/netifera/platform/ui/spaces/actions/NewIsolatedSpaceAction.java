package com.netifera.platform.ui.spaces.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.ui.IViewPart;

import com.netifera.platform.ui.internal.spaces.Activator;

public class NewIsolatedSpaceAction extends Action {

//	private final StructuredViewer viewer;
	private final SpaceCreator creator;
	
	public NewIsolatedSpaceAction(IViewPart view, StructuredViewer viewer) {
//		this.viewer = viewer;
		this.creator = new SpaceCreator(view.getSite().getWorkbenchWindow());
		setImageDescriptor(Activator.getInstance().getImageCache().getDescriptor("icons/add_space_isolated.png"));
		setText("New Isolated Space");
	}
	
	public void run() {
/* To avoid confusion, force new isolated spaces to be all top-level spaces, on the local probe.
   Dont allow isolated spaces as children of other isolated spaces for now.
		IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
		Object element = selection.getFirstElement();
		if(element instanceof IProbe) {
			IProbe probe = (IProbe) element;
			creator.openNewSpace(null, probe, true);
		} else if(element instanceof ISpace) {
			ISpace space = (ISpace) element;
			creator.openNewSpace(null, space, true);
		}
*/
		creator.openNewIsolatedSpace();
	}
}
