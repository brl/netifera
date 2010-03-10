package com.netifera.platform.ui.internal.graphs;

import com.netifera.platform.api.model.ISpace;
import com.netifera.platform.ui.spaces.visualizations.IVisualization;
import com.netifera.platform.ui.spaces.visualizations.IVisualizationProvider;

public class GraphVisualizationProvider implements IVisualizationProvider {

	public String getName() {
		return "Graph";
	}

	public IVisualization create(ISpace space) {
		return new GraphVisualization(space);
	}
}
