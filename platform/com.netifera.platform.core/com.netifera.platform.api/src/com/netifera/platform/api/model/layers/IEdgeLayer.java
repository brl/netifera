package com.netifera.platform.api.model.layers;

import java.util.List;

import com.netifera.platform.api.model.IEntity;

public interface IEdgeLayer extends ISemanticLayer {
	List<IEdge> getEdges(IEntity entity);
}
