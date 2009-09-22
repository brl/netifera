package com.netifera.platform.net.dns.model;


import com.netifera.platform.api.model.IEntity;
import com.netifera.platform.api.model.IEntityReference;
import com.netifera.platform.api.model.IWorkspace;
import com.netifera.platform.net.model.InternetAddressEntity;

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
}
