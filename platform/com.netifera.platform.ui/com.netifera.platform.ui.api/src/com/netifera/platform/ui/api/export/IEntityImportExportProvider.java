package com.netifera.platform.ui.api.export;

import com.netifera.platform.api.model.IEntity;
import com.netifera.platform.util.xml.XMLElement;

/*

<host address="1.2.3.4">
<attribute name="asdf">qwer</attribute>
<tag>asdf</tag>
</host>

 */

public interface IEntityImportExportProvider {
	IEntity importEntity(long realm, long space, XMLElement xml);
	XMLElement exportEntity(IEntity entity);
}
