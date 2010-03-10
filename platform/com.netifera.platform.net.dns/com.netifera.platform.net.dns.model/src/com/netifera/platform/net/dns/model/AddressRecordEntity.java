package com.netifera.platform.net.dns.model;

import com.netifera.platform.api.model.IEntityReference;
import com.netifera.platform.api.model.IWorkspace;
import com.netifera.platform.net.model.HostEntity;
import com.netifera.platform.net.model.InternetAddressEntity;
import com.netifera.platform.util.addresses.inet.IPv4Address;
import com.netifera.platform.util.addresses.inet.InternetAddress;

/* for A and AAAA records */
public abstract class AddressRecordEntity extends DNSRecordEntity {
	
	private static final long serialVersionUID = 3566339986137902420L;
	
	private final String name;
	protected final IEntityReference address;
	
	protected AddressRecordEntity(String typeName, IWorkspace workspace, long realmId, IEntityReference domain, String hostname, IEntityReference address) {
		super(typeName, workspace, realmId, domain);
		this.name = hostname.trim();
		this.address = address;
	}
	
	AddressRecordEntity() {
		this.name = null;
		this.address = null;
	}
	
	public String getName() {
		return name;
	}
	
	public final InternetAddressEntity getAddress() {
		return (InternetAddressEntity)referenceToEntity(address);
	}
	
	public String getFQDM() {
		return name+"."+getDomain().getFQDM();
	}
	
	@Override
	protected String generateQueryKey() {
		return createQueryKey(getTypeName(), getRealmId(), getAddress().getAddressString(), getFQDM());
	}
	
	protected static String createQueryKey(String typeName, long realmId, String address, String fqdm) {
		return typeName + ":" + realmId + ":" + address + ":" + fqdm;
	}
	
	public static synchronized AddressRecordEntity create(IWorkspace workspace, long realm, long spaceId, String fqdm, InternetAddress address) {
		fqdm = DomainEntity.normalized(fqdm);
		int dotIndex = fqdm.indexOf('.');
		
		// for hostnames without domain (like 'localhost') dont create DNS record entity nor domain entity, just add the name to the address entity
		String domain = dotIndex >= 0 ? new String(fqdm.substring(dotIndex+1)) : null;
		String hostname = dotIndex >= 0 ? new String(fqdm.substring(0, dotIndex)) : fqdm;

		AddressRecordEntity entity = null;

		if (domain != null) {
			if (address instanceof IPv4Address) {
				entity = (ARecordEntity) workspace.findByKey(ARecordEntity.createQueryKey(realm, address.toString(), fqdm));
			} else {
				entity = (AAAARecordEntity) workspace.findByKey(AAAARecordEntity.createQueryKey(realm, address.toString(), fqdm));
			}
			if(entity != null) {
				entity.getAddress().getHost().addToSpace(spaceId);
				entity.addToSpace(spaceId);
				return entity;
			}
		}
		
		// use spaceId=0 to avoid commiting to the space yet, we'll commit once the entity is tagged
		InternetAddressEntity addressEntity = InternetAddressEntity.create(workspace, realm, 0, address);
		addressEntity.addName(fqdm);
		addressEntity.save();
		HostEntity hostEntity = addressEntity.getHost();
		if (hostEntity.getLabel() == null) { //just set the first name discovered
			hostEntity.setLabel(fqdm+" ("+address+")");
		}

		if (domain != null) {
			DomainEntity domainEntity = DomainEntity.create(workspace, realm, spaceId, domain);
			
			if (!domainEntity.isTLD()) {
				hostEntity.addTag(domainEntity.getLevel(2).getFQDM());
			} else {
				hostEntity.addTag(fqdm);
			}
		
			if (address instanceof IPv4Address) {
				entity = new ARecordEntity(workspace, realm, domainEntity.createReference(), hostname, addressEntity.createReference());
			} else {
				entity = new AAAARecordEntity(workspace, realm, domainEntity.createReference(), hostname, addressEntity.createReference());
			}
			entity.save();
			entity.addToSpace(spaceId);
		}

		hostEntity.update();
		hostEntity.addToSpace(spaceId);

		return entity;
	}

}
