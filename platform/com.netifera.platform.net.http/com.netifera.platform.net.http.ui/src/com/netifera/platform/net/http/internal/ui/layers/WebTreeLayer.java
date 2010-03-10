package com.netifera.platform.net.http.internal.ui.layers;

import com.netifera.platform.api.model.IEntity;
import com.netifera.platform.api.model.layers.ITreeLayer;
import com.netifera.platform.net.http.web.model.BasicAuthenticationEntity;
import com.netifera.platform.net.http.web.model.HTTPRequestEntity;
import com.netifera.platform.net.http.web.model.WebApplicationEntity;
import com.netifera.platform.net.http.web.model.WebFormAuthenticationEntity;
import com.netifera.platform.net.http.web.model.WebPageEntity;
import com.netifera.platform.net.http.web.model.WebSiteEntity;

public class WebTreeLayer implements ITreeLayer {
	
	public IEntity[] getParents(IEntity entity) {
		if(entity instanceof WebSiteEntity) {
			return new IEntity[] {((WebSiteEntity) entity).getHTTP()};
		} else if(entity instanceof WebPageEntity) {
			return new IEntity[] {((WebPageEntity) entity).getWebSite()};
		} else if(entity instanceof WebApplicationEntity) {
			return new IEntity[] {((WebApplicationEntity) entity).getWebPage().getWebSite()};
		} else if(entity instanceof BasicAuthenticationEntity) {
			return new IEntity[] {((BasicAuthenticationEntity) entity).getWebSite()};
		} else if(entity instanceof WebFormAuthenticationEntity) {
			return new IEntity[] {((WebFormAuthenticationEntity) entity).getWebSite()};
		} else if(entity instanceof HTTPRequestEntity) {
			return new IEntity[] {((HTTPRequestEntity) entity).getConnection()};
		}
		return new IEntity[0];
	}

	public boolean isRealmRoot(IEntity entity) {
		return false;
	}

	public String getName() {
		return "Web Sites";
	}

	public boolean isDefaultEnabled() {
		return true;
	}
}
