package com.netifera.platform.ui.interop;

import com.netifera.platform.api.model.IEntity;

/*

<entity type=host id=1234 realm=12345>

<attribute name="asdf">qwer</attribute>
<association name="asdf">12345</association>
<tag>asdf</tag>



</entity>


 */
public interface IEntityImporterExporter {
	IEntity importFromXML(String xml);
	String exportToXML(IEntity entity);
}
