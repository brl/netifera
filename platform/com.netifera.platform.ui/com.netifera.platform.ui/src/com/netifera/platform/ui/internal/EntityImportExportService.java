package com.netifera.platform.ui.internal;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;

import com.netifera.platform.api.log.ILogManager;
import com.netifera.platform.api.log.ILogger;
import com.netifera.platform.api.model.AbstractEntity;
import com.netifera.platform.api.model.IEntity;
import com.netifera.platform.api.model.ISpace;
import com.netifera.platform.ui.api.export.IEntityImportExportProvider;
import com.netifera.platform.ui.api.export.IEntityImportExportService;
import com.netifera.platform.util.Base64;
import com.netifera.platform.util.xml.XMLElement;
import com.netifera.platform.util.xml.XMLParseException;

public class EntityImportExportService implements IEntityImportExportService {

	final private List<IEntityImportExportProvider> providers = new ArrayList<IEntityImportExportProvider>();
	
	private ILogger logger;
	
	public void exportEntities(Iterable<IEntity> entities, Writer writer, IProgressMonitor monitor) throws IOException {
		int totalWork = (entities instanceof ISpace) ? ((ISpace)entities).entityCount() : (entities instanceof Collection) ? ((Collection<IEntity>)entities).size() : IProgressMonitor.UNKNOWN;
		monitor.beginTask("Exporting to XML", totalWork);
		Map<Long,XMLElement> map = new HashMap<Long,XMLElement>();
		XMLElement root = new XMLElement();
		root.setName("space");
		for (IEntity entity: entities) {
			XMLElement xml = exportEntity(entity);
			monitor.worked(1);
			if (xml != null) {
				map.put(entity.getId(), xml);
				XMLElement realm = map.get(entity.getRealmId());
				if (realm != null)
					realm.addChild(xml);
				else
					root.addChild(xml);
			}
		}

		monitor.subTask("Writing XML");
		root.write(writer);
		monitor.done();
	}

	private XMLElement exportEntity(IEntity entity) throws IOException {
		for (IEntityImportExportProvider provider: providers) {
			XMLElement xml = null;
			try {
				xml = provider.exportEntity(entity);
			} catch (Exception e) {
				logger.error("Import/Export provider produced an exception when exporting "+entity, e);
			}
			if (xml != null) {
				for (String name: entity.getAttributes()) {
					XMLElement attributeElement = new XMLElement();
					attributeElement.setName("attribute");
					attributeElement.setAttribute("name", name);
					String value = entity.getAttribute(name);
					if (value.matches("(?s).*[\\x00-\\x08\\x0b-\\x0c\\x0e-\\x1f].*")) {
						attributeElement.setAttribute("encoding", "base64");
						value = Base64.encodeBytes(value.getBytes());
					}
					attributeElement.setContent(value);
					xml.addChild(attributeElement);
				}
				for (String tag: entity.getTags()) {
					XMLElement tagElement = new XMLElement();
					tagElement.setName("tag");
					tagElement.setContent(tag);
					xml.addChild(tagElement);
				}
				return xml;
			}
		}
		logger.warning("Unhandled entity: "+entity);
		return null;
	}

	public List<IEntity> importEntities(long realm, long space, Reader reader, IProgressMonitor monitor) throws XMLParseException, IOException {
		monitor.beginTask("Importing from XML", IProgressMonitor.UNKNOWN);
		monitor.subTask("Parsing XML");
		XMLElement xml = new XMLElement();
		xml.parseFromReader(reader);
		List<IEntity> entities = new ArrayList<IEntity>();
		monitor.subTask("Importing from XML");
		for (XMLElement child: xml.getChildren()) {
			IEntity entity = importEntity(realm, space, child, entities);
			if (entity != null) {
				boolean changed = false;
				for (XMLElement child2: child.getChildren()) {
					if (child2.getName().equals("attribute")) {
						String value = child2.getContent();
						String encoding = child2.getStringAttribute("encoding");
						if (encoding != null) {
							if (encoding.equals("base64")) {
								value = new String(Base64.decode(value));
							} else {
								logger.warning("Unknown attribute encoding: "+encoding);
							}
						}
						entity.setAttribute(child2.getStringAttribute("name"), value);
						changed = true;
					} else if (child2.getName().equals("tag")) {
						entity.addTag(child2.getContent());
						changed = true;
					}
				}
				if (changed)
					((AbstractEntity)entity).update(); //FIXME why must i reference AbstractEntity? should expose update() in the interface IEntity ??
				entities.add(entity);
			}
		}
		return entities;
	}

	private IEntity importEntity(long realm, long space, XMLElement xml, List<IEntity> entities) {
		for (IEntityImportExportProvider provider: providers) {
			IEntity entity = null;
			try {
				entity = provider.importEntity(realm, space, xml);
			} catch (Exception e) {
				logger.error("Import/Export provider produced an exception when importing "+xml, e);
			}
			if (entity != null) {
				if (entity.isRealmEntity()) {
					for (XMLElement child: xml.getChildren()) {
						if (!child.getName().equals("tag") && !child.getName().equals("attribute")) {
							IEntity childEntity = importEntity(entity.getId(), space, child, entities);
							if (entity != null) {
								entities.add(childEntity);
							}
						}
					}
				}
				return entity;
			}
		}
		logger.warning("Unhandled XML node: "+xml);
		return null;
	}

	protected void registerProvider(IEntityImportExportProvider provider) {
		providers.add(provider);
	}

	protected void unregisterProvider(IEntityImportExportProvider provider) {
		providers.remove(provider);
	}

	protected void setLogManager(ILogManager logManager) {
		this.logger = logManager.getLogger("Import/Export Service");
		logger.enableDebug();
	}
	
	protected void unsetLogManager(ILogManager logManager) {
	}
}
