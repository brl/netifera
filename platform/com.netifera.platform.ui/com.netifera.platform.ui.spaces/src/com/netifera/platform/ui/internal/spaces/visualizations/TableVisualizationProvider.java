package com.netifera.platform.ui.internal.spaces.visualizations;

import com.netifera.platform.api.model.ISpace;
import com.netifera.platform.ui.spaces.visualizations.IVisualization;
import com.netifera.platform.ui.spaces.visualizations.IVisualizationProvider;

public class TableVisualizationProvider implements IVisualizationProvider {

	public String getName() {
		return TableVisualization.NAME;
	}

	public IVisualization create(ISpace space) {
		return new TableVisualization(space);
	}
}
