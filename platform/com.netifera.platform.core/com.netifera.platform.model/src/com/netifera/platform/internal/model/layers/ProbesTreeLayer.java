package com.netifera.platform.internal.model.layers;

import com.netifera.platform.api.model.IEntity;
import com.netifera.platform.api.model.layers.ITreeLayer;
import com.netifera.platform.model.ProbeEntity;

public class ProbesTreeLayer implements ITreeLayer {

	public IEntity[] getParents(IEntity entity) {
		return new IEntity[0];
	}

	public boolean isRealmRoot(IEntity entity) {
		return (entity instanceof ProbeEntity) && !((ProbeEntity)entity).isLocal();
	}

	public String getName() {
		return "Probes";
	}

	public boolean isDefaultEnabled() {
		return true;
	}
}
