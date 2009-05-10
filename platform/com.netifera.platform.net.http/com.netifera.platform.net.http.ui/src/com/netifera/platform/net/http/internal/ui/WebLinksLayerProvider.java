package com.netifera.platform.net.http.internal.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.netifera.platform.api.model.IEntity;
import com.netifera.platform.api.model.layers.IEdge;
import com.netifera.platform.api.model.layers.IEdgeLayerProvider;
import com.netifera.platform.net.http.web.model.WebPageEntity;

public class WebLinksLayerProvider implements IEdgeLayerProvider {

	public List<IEdge> getEdges(final IEntity entity) {
		if (entity instanceof WebPageEntity) {
			List<IEdge> answer = new ArrayList<IEdge>();
			for (final WebPageEntity link: ((WebPageEntity) entity).getLinks())
				answer.add(new IEdge() {
					public IEntity getSource() {
						return entity;
					}
					public IEntity getTarget() {
						return link;
					}
				});
			return answer;
		}
		return Collections.emptyList();
	}

	public String getLayerName() {
		return "Web Links";
	}

	public boolean isDefaultEnabled() {
		return false;
	}
}
