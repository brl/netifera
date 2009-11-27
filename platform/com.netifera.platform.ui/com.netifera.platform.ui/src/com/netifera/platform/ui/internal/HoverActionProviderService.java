package com.netifera.platform.ui.internal;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.jface.action.IAction;

import com.netifera.platform.api.log.ILogManager;
import com.netifera.platform.api.log.ILogger;
import com.netifera.platform.ui.api.hover.IHoverActionProvider;
import com.netifera.platform.ui.api.hover.IHoverActionProviderService;

public class HoverActionProviderService implements IHoverActionProviderService {
	private final List<IHoverActionProvider> providers =
		new LinkedList<IHoverActionProvider>();

	protected void registerProvider(IHoverActionProvider provider) {
		providers.add(provider);
	}

	protected void unregisterProvider(IHoverActionProvider provider) {
		providers.remove(provider);
	}

	public List<IAction> getActions(Object o) {
		List<IAction> answer = new ArrayList<IAction>();
		for (IHoverActionProvider provider: providers)
			try {
				List<IAction> actions = provider.getActions(o);
				if (actions != null)
					answer.addAll(actions);
			} catch (Throwable exception) {
				logger.error("provider:" + provider + ", entity:" + o,
						exception);
			}
		return answer;
	}
	
	public List<IAction> getQuickActions(Object o) {
		List<IAction> answer = new ArrayList<IAction>();
		for (IHoverActionProvider provider: providers)
			try {
				List<IAction> actions = provider.getQuickActions(o);
				if (actions != null)
					answer.addAll(actions);
			} catch (Throwable exception) {
				logger.error("provider:" + provider + ", entity:" + o,
						exception);
			}
		return answer;
	}
	
	/* logging */

	private ILogger logger;
	
	protected void setLogManager(ILogManager logManager) {
		logger = logManager.getLogger("Entity Action Provider");
	}
	
	protected void unsetLogManager(ILogManager logManager) {
		logger = null;
	}
}
