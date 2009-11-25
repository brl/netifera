package com.netifera.platform.net.internal.ui.layers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.netifera.platform.api.model.IEntity;
import com.netifera.platform.api.model.layers.IEdge;
import com.netifera.platform.api.model.layers.IEdgeLayer;
import com.netifera.platform.net.model.ClientEntity;
import com.netifera.platform.net.model.ServiceEntity;

public class HostsServicesAndClientsEdgeLayer implements IEdgeLayer {

	public String getName() {
		return "Hosts, Services and Clients";
	}
	
	public boolean isDefaultEnabled() {
		return true;
	}

	public List<IEdge> getEdges(final IEntity entity) {
		if (entity instanceof ServiceEntity) {
			List<IEdge> answer = new ArrayList<IEdge>();
			answer.add(new IEdge() {
				public IEntity getSource() {
					return ((ServiceEntity)entity).getAddress().getHost();
				}
				public IEntity getTarget() {
					return entity;
				}
			});
			return answer;
		}
		if (entity instanceof ClientEntity) {
			List<IEdge> answer = new ArrayList<IEdge>();
			answer.add(new IEdge() {
				public IEntity getSource() {
					return ((ClientEntity)entity).getHost();
				}
				public IEntity getTarget() {
					return entity;
				}
			});
			return answer;
		}
		return Collections.emptyList();
	}
}
