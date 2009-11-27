package com.netifera.platform.api.model.layers;

import java.util.Set;

import com.netifera.platform.api.model.IEntity;

public interface IGroupLayer extends ISemanticLayer {
	Set<String> getGroups(IEntity entity);
}
