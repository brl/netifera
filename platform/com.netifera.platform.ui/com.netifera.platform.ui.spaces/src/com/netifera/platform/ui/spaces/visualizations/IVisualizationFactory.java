package com.netifera.platform.ui.spaces.visualizations;

import java.util.Set;

import com.netifera.platform.api.model.ISpace;

public interface IVisualizationFactory {
	Set<String> getVisualizationNames();
	IVisualization create(String name, ISpace space);
}
