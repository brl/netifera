package com.netifera.platform.ui.probe.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import com.netifera.platform.api.model.ISpace;
import com.netifera.platform.api.model.IWorkspace;
import com.netifera.platform.api.probe.IProbe;
import com.netifera.platform.ui.probe.Activator;
import com.netifera.platform.ui.spaces.SpaceEditorInput;

public class NewSpaceAction extends Action {

	private final IViewPart view;
	private final StructuredViewer viewer;
	
	public NewSpaceAction(IViewPart view, StructuredViewer viewer) {
		this.view = view;
		this.viewer = viewer;
		setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(Activator.PLUGIN_ID, "icons/new_space.png"));
		setText("New Space");
	}
	
	public void run() {
		IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
		Object element = selection.getFirstElement();
		if(!(element instanceof IProbe)) {
			return;
		}
		IProbe probe = (IProbe) element;
		openSpaceForProbe(probe);
	}
	
	private void openSpaceForProbe(IProbe probe) {
		final ISpace space = openSpace(probe);
		openEditor(space);
		viewer.setSelection(new StructuredSelection(space), true);
	}
	
	private ISpace openSpace(IProbe probe) {
		final IWorkspace workspace = Activator.getDefault().getModel().getCurrentWorkspace();
		final ISpace space = workspace.createSpace(probe.getEntity(), probe);
		space.open();
		return space;
	}
	
	private void openEditor(ISpace space) {
		final IEditorInput input = new SpaceEditorInput(space);
		try {
			view.getViewSite().getPage().openEditor(input, SpaceEditorInput.ID);
		} catch(PartInitException e) {
			// XXX
			e.printStackTrace();
		}
	}
}
