package com.netifera.platform.net.dns.model;

import java.util.Locale;

import com.netifera.platform.api.model.AbstractEntity;
import com.netifera.platform.api.model.IEntity;
import com.netifera.platform.api.model.IEntityReference;
import com.netifera.platform.api.model.IWorkspace;
import com.netifera.platform.net.model.HostEntity;
import com.netifera.platform.net.model.InternetAddressEntity;
import com.netifera.platform.net.model.ServiceEntity;
import com.netifera.platform.util.addresses.inet.InternetAddress;
import com.netifera.platform.util.addresses.inet.TCPSocketAddress;


public class EmailAddressEntity extends AbstractEntity {
	
	private static final long serialVersionUID = 8749273022330944283L;

	final public static String ENTITY_TYPE = "address.email";

	final private String address;
	final private IEntityReference domain;
	
	private String name; // should be a PersonEntity
	
	public EmailAddressEntity(IWorkspace workspace, long realmId, String address, IEntityReference domainReference) {
		super(ENTITY_TYPE, workspace, realmId);
		this.address = address;
		this.domain = domainReference;
	}
	
	EmailAddressEntity() {
		this.address = null;
		this.domain = null;
	}
	
	public String getAddress() {
		return address;
	}
	
	public DomainEntity getDomain() {
		return (DomainEntity) referenceToEntity(domain);
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}

	@Override
	protected void synchronizeEntity(AbstractEntity masterEntity) {
		EmailAddressEntity entity = (EmailAddressEntity) masterEntity;
		name = entity.name;
	}

	@Override
	protected IEntity cloneEntity() {
		EmailAddressEntity answer = new EmailAddressEntity(getWorkspace(), getRealmId(), address, domain);
		answer.name = name;
		return answer;
	}
	
	public static String createQueryKey(long realmId, String address) {
		return ENTITY_TYPE + ":" + realmId + ":" + address;
	}
	
	@Override
	protected String generateQueryKey() {
		return createQueryKey(getRealmId(), address);
	}
	
	
	public static synchronized EmailAddressEntity create(IWorkspace workspace, long realm, long spaceId, String address) {
		String accountName = address.substring(0, address.indexOf('@'));

		/*
		 * first check IP address literal (surrounded by square braces)
		 */
		if (address.contains("[")) {
			InternetAddressEntity addressEntity;
			String ip = address.substring(address.indexOf('[') + 1, address.indexOf(']'));
			InternetAddress inAddr = InternetAddress.fromString(ip);
			try {
				addressEntity = InternetAddressEntity.create(workspace, realm, spaceId, inAddr);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
				return null;
			}
			
			HostEntity hostEntity = addressEntity.getHost();
			if (hostEntity.getLabel() == null) { //XXX WTF?
				hostEntity.setLabel(ip);
				hostEntity.update();
			}

			TCPSocketAddress socketAddress = new TCPSocketAddress(inAddr, 25);
			ServiceEntity.create(workspace, realm, spaceId, socketAddress, "SMTP");

			// FIXME: no EmailAddressEntity created (because no String domain)
			
			return null;
		}
		
		/*
		 * if no IP address literal assume MX domain name.
		 * normalize domain name (but no accountname, see RFC 2821).
		 */
		String domainName = address.substring(address.indexOf('@') + 1).toLowerCase(Locale.ENGLISH);
		String normalizedAddress = accountName + "@" + domainName;

		EmailAddressEntity email = (EmailAddressEntity) workspace.findByKey(createQueryKey(realm, normalizedAddress));
		if(email != null) {
			email.addToSpace(spaceId);
			return email;
		}
	
		DomainEntity domainEntity = DomainEntity.create(workspace, realm, spaceId, domainName);
		
		email = new EmailAddressEntity(workspace, realm, normalizedAddress, domainEntity.createReference());
//		email.addTag("@"+domainName);
		email.save();
		email.addToSpace(spaceId);
		return email;
	}
	
	public static synchronized EmailAddressEntity create(IWorkspace workspace, long realm, long spaceId, String name, String address) {
		EmailAddressEntity entity = create(workspace, realm, spaceId, address);
		
		if (entity.getName() != null && entity.getName().equals(name))
			return entity; // no change
		
		entity.setName(name);
		entity.update();
		
		return entity;
	}
}
