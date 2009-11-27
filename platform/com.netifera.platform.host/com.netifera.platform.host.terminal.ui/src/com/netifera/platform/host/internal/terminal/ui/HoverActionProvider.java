package com.netifera.platform.host.internal.terminal.ui;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.action.IAction;

import com.netifera.platform.api.model.IShadowEntity;
import com.netifera.platform.host.terminal.TerminalServiceLocator;
import com.netifera.platform.host.terminal.ui.OpenTerminalViewAction;
import com.netifera.platform.ui.actions.SpaceAction;
import com.netifera.platform.ui.api.hover.IHoverActionProvider;

public class HoverActionProvider implements IHoverActionProvider {

	public List<IAction> getActions(Object o) {
		return Collections.emptyList();
	}

	public List<IAction> getQuickActions(Object o) {
		if (!(o instanceof IShadowEntity)) return Collections.emptyList();
		IShadowEntity shadow = (IShadowEntity) o;
		
		List<IAction> answer = new ArrayList<IAction>();
		
		final TerminalServiceLocator terminalLocator = (TerminalServiceLocator) shadow.getAdapter(TerminalServiceLocator.class);
		if (terminalLocator != null) {
			SpaceAction action = new OpenTerminalViewAction("Open Terminal") {
				@Override
				public URI getTerminalURL() {
					return terminalLocator.getURL();
				}
			};
			answer.add(action);
		}
		
		return answer;
	}
}
