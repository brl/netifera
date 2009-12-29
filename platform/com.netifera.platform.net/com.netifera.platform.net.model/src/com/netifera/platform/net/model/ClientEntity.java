package com.netifera.platform.net.model;

import com.netifera.platform.api.model.AbstractEntity;
import com.netifera.platform.api.model.IEntity;
import com.netifera.platform.api.model.IEntityReference;
import com.netifera.platform.api.model.IWorkspace;
import com.netifera.platform.util.addresses.inet.InternetAddress;

public class ClientEntity extends AbstractEntity {
	private static final long serialVersionUID = -1611903324378815778L;

	public final static String ENTITY_NAME = "client";
	
	private final IEntityReference host;
	private final String serviceType;
	
	public ClientEntity(IWorkspace workspace, HostEntity host, String serviceType) {
		super(ENTITY_NAME, workspace, host.getRealmId());
		this.host = host.createReference();
		this.serviceType = serviceType;
	}

	ClientEntity() {
		this.host = null;
		this.serviceType = null;
	}
	
	public HostEntity getHost() {
		return (HostEntity) referenceToEntity(host);
	}
	
	public String getServiceType() {
		return serviceType;
	}

	public String getBanner() {
		return getAttribute("banner");
	}

	public String getProduct() {
		return getAttribute("product");
	}
	
	public String getVersion() {
		return getAttribute("version");
	}
	
	public void setBanner(String banner) {
		setAttribute("banner", banner);
	}

	public void setProduct(String product) {
		setAttribute("product", product);
	}

	public void setVersion(String version) {
		setAttribute("version", version);
	}

	private ClientEntity(IWorkspace workspace, long realm, IEntityReference hostReference, String serviceType) {
		super(ENTITY_NAME, workspace, realm);
		this.host = hostReference;
		this.serviceType = serviceType;
	}
	
	protected IEntity cloneEntity() {
		return new ClientEntity(getWorkspace(), 
				getRealmId(), host, serviceType); 
	}
	
	public static String createQueryKey(long realmId, long hostId, String serviceType, String product) {
		String answer = ENTITY_NAME + ":" + realmId + ":" + hostId + ":" + serviceType;
		if (product != null)
			answer += ":" + product;
		return answer;
	}
	
	protected String generateQueryKey() {
		return createQueryKey(getRealmId(), getHost().getId(), serviceType, getProduct());
	}
	
	public static synchronized ClientEntity create(IWorkspace workspace, long realm, long spaceId, InternetAddress address, String serviceType, String product) {
		if (serviceType == null)
			throw new IllegalArgumentException("serviceType cannot be null");
		
		InternetAddressEntity addressEntity = InternetAddressEntity.create(workspace, realm, spaceId, address);
		
		ClientEntity entity = (ClientEntity) workspace.findByKey(createQueryKey(realm, addressEntity.getHost().getId(), serviceType, product));

		if (entity == null && product != null) { // XXX is this correct?
			entity = (ClientEntity) workspace.findByKey(createQueryKey(realm, addressEntity.getHost().getId(), serviceType, null));
			entity.setProduct(product);
			entity.update();
		}
		
		if (entity == null) {
			entity = new ClientEntity(workspace, addressEntity.getHost(), serviceType);
			if (product != null)
				entity.setProduct(product);
			entity.save();
		}
		
		entity.addToSpace(spaceId);
		
		return entity;
	}
}
