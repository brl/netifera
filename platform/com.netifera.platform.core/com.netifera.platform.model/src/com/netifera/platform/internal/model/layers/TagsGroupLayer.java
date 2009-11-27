package com.netifera.platform.internal.model.layers;

import java.util.Set;

import com.netifera.platform.api.model.IEntity;
import com.netifera.platform.api.model.layers.IGroupLayer;

public class TagsGroupLayer implements IGroupLayer {
	
	public Set<String> getGroups(IEntity entity) {
		return entity.getTags();
	}

	public String getName() {
		return "Tags";
	}

	public boolean isDefaultEnabled() {
		return true;
	}
}
