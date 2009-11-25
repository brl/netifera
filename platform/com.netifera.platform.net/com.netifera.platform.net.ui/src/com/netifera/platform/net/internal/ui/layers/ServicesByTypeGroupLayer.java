package com.netifera.platform.net.internal.ui.layers;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.netifera.platform.api.model.IEntity;
import com.netifera.platform.api.model.layers.IGroupLayer;
import com.netifera.platform.net.model.ServiceEntity;

public class ServicesByTypeGroupLayer implements IGroupLayer {

	public Set<String> getGroups(IEntity entity) {
		if (entity instanceof ServiceEntity) {
			String type = ((ServiceEntity)entity).getServiceType();
			if (type != null) {
				Set<String> answer = new HashSet<String>();
				answer.add(type);
				return answer;
			}
		}
		
		return Collections.emptySet();
	}
	
	public String getName() {
		return "Services By Type";
	}

	public boolean isDefaultEnabled() {
		return false;
	}
}
