package com.netifera.platform.net.http.internal.ui;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.netifera.platform.api.model.IEntity;
import com.netifera.platform.api.model.layers.IGroupLayerProvider;
import com.netifera.platform.net.http.web.model.WebPageEntity;

public class ContentTypeLayerProvider implements IGroupLayerProvider {

	public String getLayerName() {
		return "Web Pages By Content-Type";
	}

	public boolean isDefaultEnabled() {
		return false;
	}

	public Set<String> getGroups(IEntity entity) {
		if (entity instanceof WebPageEntity) {
			WebPageEntity page = (WebPageEntity) entity;
			if (page.getContentType() != null) {
				Set<String> answer = new HashSet<String>();
				answer.add(((WebPageEntity) entity).getContentType());
				return answer;
			}
		}
		return Collections.emptySet();
	}
}
