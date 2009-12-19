package com.netifera.platform.net.model;

import com.netifera.platform.api.model.AbstractEntity;
import com.netifera.platform.api.model.IEntity;
import com.netifera.platform.api.model.IEntityReference;
import com.netifera.platform.api.model.IWorkspace;
import com.netifera.platform.util.HexaEncoding;
import com.netifera.platform.util.addresses.inet.InternetAddress;


public class ServiceEntity extends AbstractEntity {
	
	private static final long serialVersionUID = 1369976797041399335L;

	public final static String ENTITY_NAME = "service";
	
	public final static String BANNER_KEY = "banner";
	public final static String PRODUCT_KEY = "product";
	public final static String VERSION_KEY = "version";

	private final int port;
	private final String protocol;
	private final IEntityReference address;
	private final String serviceType;
	
	public ServiceEntity(IWorkspace workspace, InternetAddressEntity address, int port, String protocol, String serviceType) {
		super(ENTITY_NAME, workspace, address.getRealmId());
		this.port = port;
		this.protocol = protocol;
		this.address = address.createReference();
		this.serviceType = serviceType;
	}

	ServiceEntity() {
		this.address = null;
		this.serviceType = null;
		this.protocol = null;
		this.port = 0;
	}
	public int getPort() {
		return port;
	}
	
	public String getProtocol() {
		return protocol;
	}
	
	public InternetAddressEntity getAddress() {
		return (InternetAddressEntity) referenceToEntity(address);
	}
	
	//public String getAddressString() {
	//	return addressString;
	//}
	
	public String getServiceType() {
		return serviceType;
	}

	public String getBanner() {
		return getAttribute(BANNER_KEY);
	}

	public String getProduct() {
		return getAttribute(PRODUCT_KEY);
	}
	
	public String getVersion() {
		return getAttribute(VERSION_KEY);
	}
	
	public boolean isSSL() {
		return protocol.equals("ssl");
	}

	private ServiceEntity(IWorkspace workspace, long realm, IEntityReference addressReference,
			int port, String protocol, String serviceType) {
		super(ENTITY_NAME, workspace, realm);
		this.port = port;
		this.protocol = protocol;
		this.address = addressReference.createClone();
		this.serviceType = serviceType;
	}
	
	public static String createQueryKey(long realmId, InternetAddress address, int port, String protocol) {
		return createQueryKey(realmId, HexaEncoding.bytes2hex(address.toBytes()), port, protocol);
	}
	
	private static String createQueryKey(long realmId, String addressHex, int port, String protocol) {
		return ENTITY_NAME + ":" + realmId + ":" + addressHex + ":" + port + ":" + protocol;
	}
	
	@Override
	protected String generateQueryKey() {
		return createQueryKey(getRealmId(), HexaEncoding.bytes2hex(getAddress().getData()), port, protocol);
	}
	
	protected IEntity cloneEntity() {
		return new ServiceEntity(getWorkspace(), getRealmId(), address, port,
				protocol, serviceType); 
	}
}
