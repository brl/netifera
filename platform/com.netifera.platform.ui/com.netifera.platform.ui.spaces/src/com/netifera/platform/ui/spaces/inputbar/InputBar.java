package com.netifera.platform.ui.spaces.inputbar;

import java.util.Collections;
import java.util.List;

import org.eclipse.jface.action.IAction;

import com.netifera.platform.api.model.ISpace;
import com.netifera.platform.api.probe.IProbe;
import com.netifera.platform.ui.api.inputbar.IInputBarActionProviderService;
import com.netifera.platform.ui.internal.spaces.Activator;


public class InputBar extends AbstractInputBar {
	
	public InputBar(String id) {
		super(id, Activator.getDefault().getLogManager().getLogger("Input Bar"));
	}

	protected String getDefaultToolTipText() {
		return "Enter new entity ('192.168.1.1', '192.168.1.0/24', 'www.netifera.com', '.netifera.com', 'http://netifera.com', ...)";
	}
	
	protected String getDefaultGreyedText() {
		return "Enter URL, host name, IP address, ...";
	}

	protected List<IAction> getInputActions(String content) {
		ISpace space = getActiveSpace();
		if(space == null) return Collections.emptyList();
		
		IProbe probe = Activator.getDefault().getProbeManager().getProbeById(space.getProbeId());
		if(probe == null) {
			logger.warning("No probe found for probe id = " + space.getProbeId());
			return Collections.emptyList();
		}
		
		final IInputBarActionProviderService actionProvider = Activator.getDefault().getInputBarActionProvider();
		long realm = probe.getEntity().getId();
		return actionProvider.getActions(realm, space.getId(), content);
	}
}
