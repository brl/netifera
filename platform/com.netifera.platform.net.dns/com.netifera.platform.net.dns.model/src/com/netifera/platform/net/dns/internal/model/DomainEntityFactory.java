package com.netifera.platform.net.dns.internal.model;

import com.netifera.platform.api.model.IModelService;
import com.netifera.platform.model.IWorkspaceEx;
import com.netifera.platform.net.dns.model.AAAARecordEntity;
import com.netifera.platform.net.dns.model.ARecordEntity;
import com.netifera.platform.net.dns.model.AddressRecordEntity;
import com.netifera.platform.net.dns.model.DomainEntity;
import com.netifera.platform.net.dns.model.EmailAddressEntity;
import com.netifera.platform.net.dns.model.IDomainEntityFactory;
import com.netifera.platform.net.dns.model.MXRecordEntity;
import com.netifera.platform.net.dns.model.NSRecordEntity;
import com.netifera.platform.net.dns.model.PTRRecordEntity;
import com.netifera.platform.util.addresses.inet.IPv4Address;
import com.netifera.platform.util.addresses.inet.IPv6Address;
import com.netifera.platform.util.addresses.inet.InternetAddress;

public class DomainEntityFactory implements IDomainEntityFactory {

	private IModelService model;
	
	protected void setModelService(IModelService model) {
		this.model = model;
	}

	protected void unsetModelService(IModelService model) {
		this.model = null;
	}
	
	public synchronized DomainEntity createDomain(long realm, long spaceId, String fqdm) {
		return DomainEntity.create(getWorkspace(), realm, spaceId, fqdm);
	}

	public synchronized NSRecordEntity createNSRecord(long realm, long spaceId, String domainName, String target) {
		return NSRecordEntity.create(getWorkspace(), realm, spaceId, domainName, target);
	}

	public synchronized MXRecordEntity createMXRecord(long realm, long spaceId, String domainName, String target, Integer priority) {
		return MXRecordEntity.create(getWorkspace(), realm, spaceId, domainName, target, priority);
	}

	public synchronized EmailAddressEntity createEmailAddress(long realm, long spaceId, String address) {
		return EmailAddressEntity.create(getWorkspace(), realm, spaceId, address);
	}

	public synchronized EmailAddressEntity createEmailAddress(long realm, long spaceId, String name, String address) {
		return EmailAddressEntity.create(getWorkspace(), realm, spaceId, name, address);
	}
	
	public synchronized ARecordEntity createARecord(long realm, long spaceId, String name, IPv4Address address) {
		return (ARecordEntity)createAddressRecord(realm, spaceId, name, address);
	}
	
	public synchronized AAAARecordEntity createAAAARecord(long realm, long spaceId, String name, IPv6Address address) {
		return (AAAARecordEntity)createAddressRecord(realm, spaceId, name, address);
	}
	
	private synchronized AddressRecordEntity createAddressRecord(long realm, long spaceId, String fqdm, InternetAddress address) {
		return AddressRecordEntity.create(getWorkspace(), realm, spaceId, fqdm, address);
	}
	
	public synchronized PTRRecordEntity createPTRRecord(long realm, long spaceId, InternetAddress address, String fqdm) {
		return PTRRecordEntity.create(getWorkspace(), realm, spaceId, address, fqdm);
	}
	
	private IWorkspaceEx getWorkspace() {
		if(model == null || model.getCurrentWorkspace() == null) {
			throw new IllegalStateException("Cannot create DNS entities because no workspace is currently open");
		}
		return (IWorkspaceEx) model.getCurrentWorkspace();
	}
}
