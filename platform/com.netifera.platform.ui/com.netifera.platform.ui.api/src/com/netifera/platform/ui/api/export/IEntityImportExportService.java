package com.netifera.platform.ui.api.export;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;

import com.netifera.platform.api.model.IEntity;
import com.netifera.platform.util.xml.XMLParseException;

public interface IEntityImportExportService {
	List<IEntity> importEntities(long realm, long space, Reader reader, IProgressMonitor monitor) throws XMLParseException, IOException;
	void exportEntities(Iterable<IEntity> entities, Writer writer, IProgressMonitor monitor) throws IOException;
}
