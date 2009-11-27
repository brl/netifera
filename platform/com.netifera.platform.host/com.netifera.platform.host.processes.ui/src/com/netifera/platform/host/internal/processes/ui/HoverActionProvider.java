package com.netifera.platform.host.internal.processes.ui;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.action.IAction;

import com.netifera.platform.api.log.ILogManager;
import com.netifera.platform.api.log.ILogger;
import com.netifera.platform.api.model.IShadowEntity;
import com.netifera.platform.host.processes.ProcessServiceLocator;
import com.netifera.platform.host.processes.ui.OpenProcessListAction;
import com.netifera.platform.ui.actions.SpaceAction;
import com.netifera.platform.ui.api.actions.IHoverActionProvider;

public class HoverActionProvider implements IHoverActionProvider {

	private ILogger logger;
	
	public List<IAction> getActions(Object o) {
		return Collections.emptyList();
	}

	public List<IAction> getQuickActions(Object o) {
		if (!(o instanceof IShadowEntity)) return Collections.emptyList();
		IShadowEntity shadow = (IShadowEntity) o;

		List<IAction> answer = new ArrayList<IAction>();
		
		final ProcessServiceLocator processServiceLocator = (ProcessServiceLocator) shadow.getAdapter(ProcessServiceLocator.class);
		if (processServiceLocator != null) {
			SpaceAction action = new OpenProcessListAction("Process List") {
				@Override
				public URI getProcessServiceURL() {
					return processServiceLocator.getURL();
				}
			};
			answer.add(action);
		}
		
		return answer;
	}
	
	protected void setLogManager(ILogManager logManager) {
		logger = logManager.getLogger("Process Service");
	}
	
	protected void unsetLogManager(ILogManager logManager) {
		
	}
}
