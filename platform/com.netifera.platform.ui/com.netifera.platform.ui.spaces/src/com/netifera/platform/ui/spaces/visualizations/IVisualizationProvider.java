package com.netifera.platform.ui.spaces.visualizations;

import com.netifera.platform.api.model.ISpace;

public interface IVisualizationProvider {
	String getName();
	IVisualization create(ISpace space);
}
