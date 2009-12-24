package com.netifera.platform.net.ui.geoip;

import com.netifera.platform.api.model.IEntity;
import com.netifera.platform.api.model.layers.ISemanticLayer;

public interface IGeographicalLayer extends ISemanticLayer {
	ILocation getLocation(IEntity entity);
}
