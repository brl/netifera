package com.netifera.platform.api.model.layers;

import com.netifera.platform.api.model.IEntity;

public interface ITreeLayer extends ISemanticLayer {
	boolean isRealmRoot(IEntity entity);
	IEntity[] getParents(IEntity entity);
}
