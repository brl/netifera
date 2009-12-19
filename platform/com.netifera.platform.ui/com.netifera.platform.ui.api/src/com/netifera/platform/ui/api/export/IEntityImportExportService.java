package com.netifera.platform.ui.api.export;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Collection;
import java.util.List;

import com.netifera.platform.api.model.IEntity;
import com.netifera.platform.util.xml.XMLParseException;

public interface IEntityImportExportService {
	List<IEntity> importEntities(long realm, long space, Reader reader) throws XMLParseException, IOException;
	void exportEntities(Collection<IEntity> entities, Writer writer) throws IOException;
}
