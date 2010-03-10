package com.netifera.platform.api.model;

import java.util.List;

public interface ITreeStructureContext extends IStructureContext {
	IShadowEntity getParent();
	List<IShadowEntity> getChildren();
}
