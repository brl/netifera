package com.netifera.platform.net.internal.ui.layers;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.netifera.platform.api.model.IEntity;
import com.netifera.platform.api.model.layers.IGroupLayer;
import com.netifera.platform.net.model.HostEntity;

public class OperatingSystemGroupLayer implements IGroupLayer {

	public Set<String> getGroups(IEntity entity) {
		if (entity instanceof HostEntity) {
			String os = ((HostEntity)entity).getAttribute("os");
			if (os != null) {
				Set<String> answer = new HashSet<String>();
				answer.add(os);
				return answer;
			}
		}
		
		return Collections.emptySet();
	}
	
	public String getName() {
		return "Hosts By Operating System";
	}

	public boolean isDefaultEnabled() {
		return false;
	}
}
