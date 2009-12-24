package com.netifera.platform.net.dns.model;


import com.netifera.platform.api.model.IEntity;
import com.netifera.platform.api.model.IEntityReference;
import com.netifera.platform.api.model.IWorkspace;
import com.netifera.platform.net.model.HostEntity;
import com.netifera.platform.net.model.InternetAddressEntity;
import com.netifera.platform.util.addresses.inet.InternetAddress;

public class PTRRecordEntity extends DNSRecordEntity {
	
	private static final long serialVersionUID = 5885077675573832948L;

	final public static String ENTITY_TYPE = "dns.ptr";

	final private IEntityReference address;
	final private String name;
	
	public PTRRecordEntity(IWorkspace workspace, long realmId, IEntityReference domain, IEntityReference address, String name) {
		super(ENTITY_TYPE, workspace, realmId, domain);
		this.address = address.createClone();
		this.name = name;
	}
	
	PTRRecordEntity() {
		address = null;
		name = null;
	}
	
	public String getName() {
		return name;
	}

	public String getFQDM() {
		return name+"."+getDomain().getFQDM();
	}

	public InternetAddressEntity getAddress() {
		return (InternetAddressEntity)referenceToEntity(address);
	}
	
	public static String createQueryKey(long realmId, String address, String fqdm) {
		return ENTITY_TYPE + ":" + realmId + ":" + address + ":" + fqdm;
	}
	
	@Override
	protected String generateQueryKey() {
		return createQueryKey(getRealmId(), getAddress().getAddressString(), getFQDM());
	}
	
	@Override
	protected IEntity cloneEntity() {
		return new PTRRecordEntity(getWorkspace(), getRealmId(), domain, address, name);
	}
	
	public static synchronized PTRRecordEntity create(IWorkspace workspace, long realm, long spaceId, InternetAddress address, String fqdm) {
		fqdm = DomainEntity.normalized(fqdm);
		int dotIndex = fqdm.indexOf('.');
		// for hostnames without domain (like 'localhost') dont create DNS record entity nor domain entity, just add the name to the address entity
		String domain = dotIndex >= 0 ? new String(fqdm.substring(dotIndex+1)) : null;
		String hostname = dotIndex >= 0 ? new String(fqdm.substring(0, dotIndex)) : fqdm;

		if (fqdm.endsWith(".arpa")) { // invalid PTR entry (mostly error in bind zone
			return null;
		}
		String addressString = address.toString();

		PTRRecordEntity entity = null;

		if (domain != null) {
			entity = (PTRRecordEntity) workspace.findByKey(PTRRecordEntity.createQueryKey(realm, addressString, fqdm));
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
	
			if (!domainEntity.isTLD())
				hostEntity.addTag(domainEntity.getLevel(2).getFQDM());
			else
				hostEntity.addTag(fqdm);

			entity = new PTRRecordEntity(workspace, realm, domainEntity.createReference(), addressEntity.createReference(), hostname);
			entity.save();
			entity.addToSpace(spaceId);
		}
		
		hostEntity.update();
		hostEntity.addToSpace(spaceId);

		return entity;
	}
}
