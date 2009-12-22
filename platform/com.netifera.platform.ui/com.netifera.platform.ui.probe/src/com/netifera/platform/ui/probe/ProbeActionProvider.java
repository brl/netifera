package com.netifera.platform.ui.probe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.wizard.WizardDialog;

import com.netifera.platform.api.model.AbstractEntity;
import com.netifera.platform.api.probe.IProbe;
import com.netifera.platform.model.ProbeEntity;
import com.netifera.platform.net.model.HostEntity;
import com.netifera.platform.ui.actions.SpaceAction;
import com.netifera.platform.ui.api.actions.ISpaceAction;
import com.netifera.platform.ui.api.hover.IHoverActionProvider;
import com.netifera.platform.ui.probe.wizard.NewProbeWizard;
import com.netifera.platform.ui.spaces.actions.SpaceCreator;

public class ProbeActionProvider implements IHoverActionProvider {

	public List<IAction> getActions(Object o) {
		return Collections.emptyList();
	}

	public List<IAction> getQuickActions(Object o) {
		if (o instanceof HostEntity) {
			final HostEntity hostEntity = (HostEntity) (((AbstractEntity) o).getRealEntity());

			List<IAction> actions = new ArrayList<IAction>();
			
			ISpaceAction newProbeAction = new SpaceAction("New Probe") {
				public void run() {
					NewProbeWizard wizard = new NewProbeWizard(getSpace().getId(), hostEntity);
					WizardDialog dialog  = new WizardDialog(Activator.getInstance().getWorkbench().getActiveWorkbenchWindow().getShell(), wizard);
					dialog.open();
				}
			};
			newProbeAction.setImageDescriptor(Activator.getInstance().getImageCache().getDescriptor("icons/new_probe.png"));
			actions.add(newProbeAction);
			
			return actions;
		}
		
		if (o instanceof ProbeEntity) {
			final IProbe probe = Activator.getInstance().getProbeManager().getProbeById(((ProbeEntity)o).getProbeId());
	
			if (probe == null)
				return Collections.emptyList();
	
			List<IAction> actions = new ArrayList<IAction>();
			
			if (!probe.isLocalProbe() && probe.isConnected()) {
				ISpaceAction disconnectAction = new SpaceAction("Disconnect Probe") {
					public void run() {
						probe.disconnect();
					}
				};
				disconnectAction.setImageDescriptor(Activator.getInstance().getImageCache().getDescriptor("icons/disconnect.png"));
				actions.add(disconnectAction);
			}
			
			if (!probe.isLocalProbe() && !probe.isConnected()) {
				ISpaceAction connectAction = new SpaceAction("Connect Probe") {
					public void run() {
						probe.connect();
					}
				};
				connectAction.setImageDescriptor(Activator.getInstance().getImageCache().getDescriptor("icons/connect.png"));
				actions.add(connectAction);
			}

			final SpaceCreator spaceCreator = new SpaceCreator(Activator.getInstance().getWorkbench().getActiveWorkbenchWindow());
			
			ISpaceAction newSpaceAction = new SpaceAction("New Space") {
				public void run() {
					spaceCreator.openNewSpace(null, probe, false);
				}
			};
			newSpaceAction.setImageDescriptor(Activator.getInstance().getImageCache().getDescriptor("icons/add_space.png"));
			actions.add(newSpaceAction);

			return actions;
		}
		
		return Collections.emptyList();
	}
}
