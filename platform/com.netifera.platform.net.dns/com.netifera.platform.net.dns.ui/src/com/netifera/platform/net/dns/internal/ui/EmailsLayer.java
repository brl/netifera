package com.netifera.platform.net.dns.internal.ui;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.netifera.platform.api.model.IEntity;
import com.netifera.platform.api.model.layers.IGroupLayer;
import com.netifera.platform.net.dns.model.EmailAddressEntity;

public class EmailsLayer implements IGroupLayer {

	public String getName() {
		return "Emails by Domain";
	}

	public boolean isDefaultEnabled() {
		return false;
	}

	public Set<String> getGroups(IEntity entity) {
		if(entity instanceof EmailAddressEntity) {
			Set<String> answer = new HashSet<String>();
			answer.add(((EmailAddressEntity) entity).getDomain().getFQDM());
			return answer;
		}
		return Collections.emptySet();
	}
}
