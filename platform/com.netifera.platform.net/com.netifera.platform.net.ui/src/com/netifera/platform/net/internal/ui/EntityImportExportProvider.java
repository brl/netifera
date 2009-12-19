package com.netifera.platform.net.internal.ui;

import com.netifera.platform.api.model.IEntity;
import com.netifera.platform.net.model.ClientEntity;
import com.netifera.platform.net.model.HostEntity;
import com.netifera.platform.net.model.INetworkEntityFactory;
import com.netifera.platform.net.model.InternetAddressEntity;
import com.netifera.platform.net.model.NetblockEntity;
import com.netifera.platform.net.model.ServiceEntity;
import com.netifera.platform.ui.api.export.IEntityImportExportProvider;
import com.netifera.platform.util.addresses.inet.InternetAddress;
import com.netifera.platform.util.xml.XMLElement;

public class EntityImportExportProvider implements IEntityImportExportProvider {

	private INetworkEntityFactory entityFactory;
	
	public XMLElement exportEntity(IEntity entity) {
/*		if (entity instanceof AddressEntity) {
			AddressEntity hostEntity = (AddressEntity) entity;
			XMLElement xml = new XMLElement();
			xml.setName("host");
			xml.setAttribute("address", hostEntity.getDefaultAddress().getAddress().toString());
			return xml;
		} else */ if (entity instanceof NetblockEntity) {
			XMLElement xml = new XMLElement();
			xml.setName("netblock");
			xml.setContent(((NetblockEntity)entity).getNetblock().toString());
		} else if (entity instanceof HostEntity) {
			HostEntity hostEntity = (HostEntity) entity;
			XMLElement xml = new XMLElement();
			xml.setName("host");
			xml.setAttribute("address", hostEntity.getDefaultAddress().toNetworkAddress().toString());
			return xml;
		} else if (entity instanceof ServiceEntity) {
			ServiceEntity serviceEntity = (ServiceEntity) entity;
			XMLElement xml = new XMLElement();
			xml.setName("service");
			if (serviceEntity.getServiceType() != null) xml.setAttribute("type", serviceEntity.getServiceType());
			xml.setAttribute("location", serviceEntity.getAddress().toNetworkAddress()+":"+serviceEntity.getPort()+"/"+serviceEntity.getProtocol());
			return xml;
		} else if (entity instanceof ClientEntity) {
			ClientEntity clientEntity = (ClientEntity) entity;
			XMLElement xml = new XMLElement();
			xml.setName("client");
			xml.setAttribute("type", clientEntity.getServiceType());
			xml.setAttribute("host", clientEntity.getHost().getDefaultAddress().toNetworkAddress().toString());
			return xml;
/*		} else if (entity instanceof CredentialEntity) {
			CredentialEntity credentialEntity = (CredentialEntity) entity;
			if (credentialEntity.getAuthenticable() instanceof ServiceEntity) {
				ServiceEntity serviceEntity = (ServiceEntity) credentialEntity.getAuthenticable();
				XMLElement xml = new XMLElement();
				xml.setName("userpassword");
				xml.setAttribute("service", credentialEntity.getAuthenticable().getServiceType());
				return xml;
			}
*/		}
		return null;
	}

	public IEntity importEntity(long realm, long space, XMLElement xml) {
		if (xml.getName().equals("host")) {
			InternetAddress address = InternetAddress.fromString(xml.getStringAttribute("address"));
			InternetAddressEntity addressEntity = entityFactory.createAddress(realm, space, address);
			return addressEntity.getHost();
		} else if (xml.getName().equals("service")) {		
//			entityFactory.createService(realm, space, locator, serviceType, info)
		}
		return null;
	}

	protected void setEntityFactory(INetworkEntityFactory entityFactory) {
		this.entityFactory = entityFactory;
	}

	protected void unsetEntityFactory(INetworkEntityFactory entityFactory) {
		this.entityFactory = null;
	}
}
