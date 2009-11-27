package com.netifera.platform.net.internal.ui.layers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.netifera.platform.api.model.IEntity;
import com.netifera.platform.api.model.layers.IEdge;
import com.netifera.platform.api.model.layers.IEdgeLayer;
import com.netifera.platform.net.model.ClientServiceConnectionEntity;

public class ConnectionsEdgeLayer implements IEdgeLayer {

	public String getName() {
		return "Client-Service Connections";
	}
	
	public boolean isDefaultEnabled() {
		return true;
	}

	public List<IEdge> getEdges(final IEntity entity) {
		if (entity instanceof ClientServiceConnectionEntity) {
			List<IEdge> answer = new ArrayList<IEdge>();
			answer.add(new IEdge() {
				public IEntity getSource() {
					return ((ClientServiceConnectionEntity)entity).getClient().getHost();
				}
				public IEntity getTarget() {
					return ((ClientServiceConnectionEntity)entity).getService().getAddress().getHost();
				}
			});
			return answer;
		}
		return Collections.emptyList();
	}
}
