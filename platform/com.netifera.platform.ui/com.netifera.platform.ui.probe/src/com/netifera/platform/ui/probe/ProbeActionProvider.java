package com.netifera.platform.ui.probe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.action.IAction;

import com.netifera.platform.api.log.ILogManager;
import com.netifera.platform.api.log.ILogger;
import com.netifera.platform.api.probe.IProbe;
import com.netifera.platform.api.probe.IProbe.ConnectState;
import com.netifera.platform.model.ProbeEntity;
import com.netifera.platform.ui.actions.SpaceAction;
import com.netifera.platform.ui.api.actions.ISpaceAction;
import com.netifera.platform.ui.api.hover.IHoverActionProvider;

public class ProbeActionProvider implements IHoverActionProvider {

	private ILogger logger;
	
	public List<IAction> getActions(Object o) {
		return Collections.emptyList();
	}

	public List<IAction> getQuickActions(Object o) {
		if (!(o instanceof ProbeEntity)) return Collections.emptyList();

		final IProbe probe = Activator.getInstance().getProbeManager().getProbeById(((ProbeEntity)o).getProbeId());
		if(probe == null || probe.isLocalProbe()) return Collections.emptyList();

		List<IAction> actions = new ArrayList<IAction>();
		
		if(probe.getConnectState() == ConnectState.CONNECTED) {
			ISpaceAction disconnectAction = new SpaceAction("Disconnect Probe") {
				public void run() {
					probe.disconnect();
				}
			};
			disconnectAction.setImageDescriptor(Activator.getInstance().getImageCache().getDescriptor("icons/disconnect.png"));

			actions.add(disconnectAction);
		}
		
		if(probe.getConnectState() != ConnectState.CONNECTED) {
			ISpaceAction connectAction = new SpaceAction("Connect Probe") {
				public void run() {
					probe.connect();
				}
			};
			connectAction.setImageDescriptor(Activator.getInstance().getImageCache().getDescriptor("icons/connect.png"));
			actions.add(connectAction);
		}
		return actions;
	}
	
	protected void setLogManager(ILogManager logManager) {
		logger = logManager.getLogger("Probe Action");
	}
	
	protected void unsetLogManager(ILogManager logManager) {
		
	}
}
