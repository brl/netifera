package com.netifera.platform.ui.probe.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import com.netifera.platform.ui.probe.Activator;
import com.netifera.platform.ui.probe.wizard.NewProbeWizard;

public class NewProbeAction extends Action {
	private final StructuredViewer viewer;
	
	public NewProbeAction(StructuredViewer viewer) {
		this.viewer = viewer;
		setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(Activator.PLUGIN_ID, "icons/new_probe.png"));
		setText("New Probe");
	}
	
	@Override
	public void run() {
/*		IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
		Object element = selection.getFirstElement();
		if(!(element instanceof IProbe)) {
			return;
		}
		IProbe probe = (IProbe) element;
*/
		//FIXME the new probe should be child of the selected probe
		
		NewProbeWizard wizard = new NewProbeWizard();
		WizardDialog dialog  = new WizardDialog(viewer.getControl().getShell(), wizard);
		dialog.open();
	}
}
