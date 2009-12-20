package com.netifera.platform.net.internal.ui;

import com.netifera.platform.api.model.IEntity;
import com.netifera.platform.net.model.ClientEntity;
import com.netifera.platform.net.model.CredentialEntity;
import com.netifera.platform.net.model.HostEntity;
import com.netifera.platform.net.model.INetworkEntityFactory;
import com.netifera.platform.net.model.InternetAddressEntity;
import com.netifera.platform.net.model.NetblockEntity;
import com.netifera.platform.net.model.PasswordEntity;
import com.netifera.platform.net.model.ServiceEntity;
import com.netifera.platform.net.model.UserEntity;
import com.netifera.platform.net.model.UsernameAndPasswordEntity;
import com.netifera.platform.ui.api.export.IEntityImportExportProvider;
import com.netifera.platform.util.addresses.inet.InternetAddress;
import com.netifera.platform.util.addresses.inet.InternetNetblock;
import com.netifera.platform.util.addresses.inet.InternetSocketAddress;
import com.netifera.platform.util.xml.XMLElement;

public class EntityImportExportProvider implements IEntityImportExportProvider {

	private INetworkEntityFactory entityFactory;
	
	public XMLElement exportEntity(IEntity entity) {
		if (entity instanceof InternetAddressEntity) {
			InternetAddressEntity addressEntity = (InternetAddressEntity) entity;
			XMLElement xml = new XMLElement();
			xml.setName("address");
			xml.setAttribute("value", addressEntity.toNetworkAddress().toString());
			for (String name: addressEntity.getNames()) {
				XMLElement nameXml = new XMLElement();
				nameXml.setName("name");
				nameXml.setContent(name);
				xml.addChild(nameXml);
			}
			return xml;
		} else if (entity instanceof NetblockEntity) {
			XMLElement xml = new XMLElement();
			xml.setName("netblock");
			xml.setAttribute("value", ((NetblockEntity)entity).getNetblock().toString());
			return xml;
		} else if (entity instanceof HostEntity) {
			HostEntity hostEntity = (HostEntity) entity;
			XMLElement xml = new XMLElement();
			xml.setName("host");
			xml.setAttribute("address", hostEntity.getDefaultAddress().toNetworkAddress().toString());
			//TODO add extra addresses
			return xml;
		} else if (entity instanceof ServiceEntity) {
			ServiceEntity serviceEntity = (ServiceEntity) entity;
			XMLElement xml = new XMLElement();
			xml.setName("service");
			xml.setAttribute("address", serviceEntity.toSocketAddress().toString());
			if (serviceEntity.getServiceType() != null) xml.setAttribute("type", serviceEntity.getServiceType());
			return xml;
		} else if (entity instanceof ClientEntity) {
			ClientEntity clientEntity = (ClientEntity) entity;
			XMLElement xml = new XMLElement();
			xml.setName("client");
			xml.setAttribute("host", clientEntity.getHost().getDefaultAddress().toNetworkAddress().toString());
			xml.setAttribute("type", clientEntity.getServiceType());
			return xml;
		} else if (entity instanceof UserEntity) {
			UserEntity userEntity = (UserEntity) entity;
			XMLElement xml = new XMLElement();
			xml.setName("user");
			xml.setAttribute("username", userEntity.getName());
			xml.setAttribute("host", userEntity.getHost().getDefaultAddress().toNetworkAddress().toString());
			return xml;
		} else if (entity instanceof CredentialEntity) {
			CredentialEntity credentialEntity = (CredentialEntity) entity;
			if (credentialEntity.getAuthenticable() instanceof ServiceEntity) {
				ServiceEntity serviceEntity = (ServiceEntity) credentialEntity.getAuthenticable();
				if (credentialEntity instanceof UsernameAndPasswordEntity) {
					XMLElement xml = new XMLElement();
					xml.setName("usernamepassword");
					xml.setAttribute("service", serviceEntity.toSocketAddress().toString());
					xml.setAttribute("username", ((UsernameAndPasswordEntity)credentialEntity).getUsername());
					xml.setAttribute("password", ((UsernameAndPasswordEntity)credentialEntity).getPassword());
					return xml;
				} else if (credentialEntity instanceof PasswordEntity) {
					XMLElement xml = new XMLElement();
					xml.setName("password");
					xml.setAttribute("service", serviceEntity.toSocketAddress().toString());
					xml.setAttribute("password", ((PasswordEntity)credentialEntity).getPassword());
					return xml;
				}
			}
		}
		return null;
	}

	public IEntity importEntity(long realm, long space, XMLElement xml) {
		if (xml.getName().equals("address")) {
			InternetAddressEntity addressEntity = entityFactory.createAddress(realm, space, InternetAddress.fromString(xml.getStringAttribute("value")));
			for (XMLElement child: xml.getChildren()) {
				if (child.getName().equals("name")) {
					addressEntity.addName(child.getContent());
				}
			}
			if (addressEntity.getNames().size()>0) {
				addressEntity.update();
			}
			return addressEntity;
		} else if (xml.getName().equals("netblock")) {		
			return entityFactory.createNetblock(realm, space, InternetNetblock.fromString(xml.getStringAttribute("value")));
		} else if (xml.getName().equals("host")) {
			InternetAddress address = InternetAddress.fromString(xml.getStringAttribute("address"));
			InternetAddressEntity addressEntity = entityFactory.createAddress(realm, space, address);
			return addressEntity.getHost();
		} else if (xml.getName().equals("service")) {		
			return entityFactory.createService(realm, space, InternetSocketAddress.fromString(xml.getStringAttribute("address")), xml.getStringAttribute("type"), null);
		} else if (xml.getName().equals("client")) {
			return entityFactory.createClient(realm, space, InternetAddress.fromString(xml.getStringAttribute("host")), xml.getStringAttribute("type"), null, null);
		} else if (xml.getName().equals("user")) {
			return entityFactory.createUser(realm, space, InternetAddress.fromString(xml.getStringAttribute("host")), xml.getStringAttribute("username"));
		} else if (xml.getName().equals("usernamepassword")) {
			return entityFactory.createUsernameAndPassword(realm, space, InternetSocketAddress.fromString(xml.getStringAttribute("service")), xml.getStringAttribute("username"), xml.getStringAttribute("password"));
		} else if (xml.getName().equals("password")) {
			return entityFactory.createPassword(realm, space, InternetSocketAddress.fromString(xml.getStringAttribute("service")), xml.getStringAttribute("password"));
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
