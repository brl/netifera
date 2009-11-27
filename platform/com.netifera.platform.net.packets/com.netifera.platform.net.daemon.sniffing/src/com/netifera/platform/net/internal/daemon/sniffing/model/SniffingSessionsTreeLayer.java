package com.netifera.platform.net.internal.daemon.sniffing.model;

import com.netifera.platform.api.model.IEntity;
import com.netifera.platform.api.model.layers.ITreeLayer;
import com.netifera.platform.net.daemon.sniffing.model.CaptureFileEntity;
import com.netifera.platform.net.daemon.sniffing.model.NetworkInterfaceEntity;
import com.netifera.platform.net.daemon.sniffing.model.SniffingSessionEntity;

public class SniffingSessionsTreeLayer implements ITreeLayer {

	public IEntity[] getParents(IEntity entity) {
		return new IEntity[0];
	}

	public boolean isRealmRoot(IEntity entity) {
		return (entity instanceof SniffingSessionEntity) ||
		(entity instanceof CaptureFileEntity) ||
		(entity instanceof NetworkInterfaceEntity);
	}

	public String getName() {
		return "Sniffing Sessions";
	}

	public boolean isDefaultEnabled() {
		return true;
	}
}
