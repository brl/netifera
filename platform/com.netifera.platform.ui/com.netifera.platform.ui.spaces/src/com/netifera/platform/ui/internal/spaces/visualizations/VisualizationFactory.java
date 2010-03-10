package com.netifera.platform.ui.internal.spaces.visualizations;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.netifera.platform.api.model.ISpace;
import com.netifera.platform.ui.spaces.visualizations.IVisualization;
import com.netifera.platform.ui.spaces.visualizations.IVisualizationFactory;
import com.netifera.platform.ui.spaces.visualizations.IVisualizationProvider;

public class VisualizationFactory implements IVisualizationFactory {
	private final Map<String,IVisualizationProvider> providers = new HashMap<String,IVisualizationProvider>();
	
	public synchronized IVisualization create(String name, ISpace space) {
		return providers.get(name).create(space);
	}
	
	public synchronized Set<String> getVisualizationNames() {
		return providers.keySet();
	}
	
	protected synchronized void registerProvider(IVisualizationProvider provider) {
		providers.put(provider.getName(), provider);
	}
	
	protected synchronized void unregisterProvider(IVisualizationProvider provider) {
		providers.remove(provider.getName());
	}
}
