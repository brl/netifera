package com.netifera.platform.net.wifi.internal.model;

import com.netifera.platform.api.model.IEntity;
import com.netifera.platform.api.model.layers.ITreeLayer;
import com.netifera.platform.net.wifi.model.AccessPointEntity;
import com.netifera.platform.net.wifi.model.ExtendedServiceSetEntity;

public class WirelessNetworksTreeLayer implements ITreeLayer {

	public IEntity[] getParents(IEntity entity) {
		if(entity instanceof AccessPointEntity) {
			final AccessPointEntity ap = (AccessPointEntity) entity;
			return new IEntity[] { ap.getESS() };
		}
		
		
		return new IEntity[0];
	}

	public boolean isRealmRoot(IEntity entity) {
		return (entity instanceof ExtendedServiceSetEntity);
	}

	public String getName() {
		return "Wireless Networks";
	}

	public boolean isDefaultEnabled() {
		return true;
	}

}
