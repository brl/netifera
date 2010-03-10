package com.netifera.platform.ui.api.export;

import com.netifera.platform.api.model.IEntity;
import com.netifera.platform.util.xml.XMLElement;

public interface IEntityImportExportProvider {
	IEntity importEntity(long realm, long space, XMLElement xml);
	XMLElement exportEntity(IEntity entity);
}
