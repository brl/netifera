package com.netifera.platform.ui.internal;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.IAction;

import com.netifera.platform.api.log.ILogManager;
import com.netifera.platform.api.log.ILogger;
import com.netifera.platform.ui.api.hover.IHoverActionProvider;
import com.netifera.platform.ui.api.hover.IHoverInformationProvider;
import com.netifera.platform.ui.api.hover.IHoverService;

public class HoverService implements IHoverService {
	private final List<IHoverActionProvider> actionProviders = new ArrayList<IHoverActionProvider>();
	private final List<IHoverInformationProvider> informationProviders = new ArrayList<IHoverInformationProvider>();

	public String getInformation(Object o) {
		StringBuffer buffer = new StringBuffer();
		for(IHoverInformationProvider provider : informationProviders) {
			String information = provider.getInformation(o);
			if(information != null)
				buffer.append(information);
		}
		return buffer.toString();
	}

	public List<IAction> getActions(Object o) {
		List<IAction> answer = new ArrayList<IAction>();
		for (IHoverActionProvider provider: actionProviders)
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
		for (IHoverActionProvider provider: actionProviders)
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
	
	protected void registerActionProvider(IHoverActionProvider provider) {
		actionProviders.add(provider);
	}

	protected void unregisterActionProvider(IHoverActionProvider provider) {
		actionProviders.remove(provider);
	}

	protected void registerInformationProvider(IHoverInformationProvider provider) {
		informationProviders.add(provider);
	}

	protected void unregisterInformationProvider(IHoverInformationProvider provider) {
		informationProviders.remove(provider);
	}
}
