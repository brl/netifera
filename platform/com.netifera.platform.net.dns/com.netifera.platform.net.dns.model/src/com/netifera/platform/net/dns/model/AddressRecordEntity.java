package com.netifera.platform.net.dns.model;

import com.netifera.platform.api.model.IEntityReference;
import com.netifera.platform.api.model.IWorkspace;
import com.netifera.platform.net.model.InternetAddressEntity;

/* for A and AAAA records */
abstract class AddressRecordEntity extends DNSRecordEntity {
	
	private static final long serialVersionUID = 3566339986137902420L;
	
	private final String name;
	protected final IEntityReference address;
	
	protected AddressRecordEntity(String typeName, IWorkspace workspace, long realmId, IEntityReference domain, String hostname, IEntityReference address) {
		super(typeName, workspace, realmId, domain);
		this.name = hostname.trim();
		this.address = address.createClone();
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
}
