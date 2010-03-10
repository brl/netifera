package com.netifera.platform.net.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.netifera.platform.api.model.AbstractEntity;
import com.netifera.platform.api.model.IEntity;
import com.netifera.platform.api.model.IEntityReference;
import com.netifera.platform.api.model.IWorkspace;
import com.netifera.platform.util.addresses.inet.InternetAddress;

public class InternetAddressEntity extends NetworkAddressEntity {
	
	private static final long serialVersionUID = -4283823680888685989L;

	public final static String ENTITY_NAME = "address.ip";

	public final static String OPEN_TCP_PORTS_KEY = "openTCPPorts";
	public final static String CLOSED_TCP_PORTS_KEY = "closedTCPPorts";
	public final static String OPEN_UDP_PORTS_KEY = "openUDPPorts";
	public final static String CLOSED_UDP_PORTS_KEY = "closedUDPPorts";
	
	private final IEntityReference host;
	
	private Set<String> names = new HashSet<String>();
	
	public InternetAddressEntity(IWorkspace workspace, HostEntity host, String address) {
		super(ENTITY_NAME, workspace, host.getRealmId(), InternetAddress.fromString(address).toBytes());
		this.host = host.createReference();
	}

	private InternetAddressEntity(IWorkspace workspace, IEntityReference hostReference, long realmId, byte[] address) {
		super(ENTITY_NAME, workspace, realmId, address);
		this.host = hostReference;
	}
	
	InternetAddressEntity() {
		host = null;
	}
	
	public InternetAddress toNetworkAddress() {
		return InternetAddress.fromBytes(getData());
	}

	public String getOpenTCPPorts() {
		return getAttribute(OPEN_TCP_PORTS_KEY);
	}

	public String getClosedTCPPorts() {
		return getAttribute(CLOSED_TCP_PORTS_KEY);
	}

	public String getOpenUDPPorts() {
		return getAttribute(OPEN_UDP_PORTS_KEY);
	}

	public String getClosedUDPPorts() {
		return getAttribute(CLOSED_UDP_PORTS_KEY);
	}

	public HostEntity getHost() {
		return (HostEntity) referenceToEntity(host);
	}

	public void addName(String name) {
		names.add(name);
	}
	
	public Set<String> getNames() {
		return Collections.unmodifiableSet(names);
	}
	
	@Override
	protected void synchronizeEntity(AbstractEntity masterEntity) {
		super.synchronizeEntity(masterEntity);
		names = ((InternetAddressEntity)masterEntity).names;
	}
	
	protected IEntity cloneEntity() {
		InternetAddressEntity clone = new InternetAddressEntity(getWorkspace(), host, getRealmId(), getData());
		clone.names = names;
		return clone;
	}
	
	public static String createQueryKey(long realmId, InternetAddress address) {
		return NetworkAddressEntity.createQueryKey(ENTITY_NAME, realmId, address.toBytes());
	}
	
	public static synchronized InternetAddressEntity create(IWorkspace workspace, long realm, long spaceId, InternetAddress address) {
		InternetAddressEntity addr = (InternetAddressEntity) workspace.findByKey(createQueryKey(realm, address));
		if(addr != null) {
			addr.getHost().addToSpace(spaceId);
			addr.addToSpace(spaceId);
			return addr;
		}
		
		HostEntity hostEntity = new HostEntity(workspace, realm);
		
		// First the HostEntity must be saved so that InternetAddressEntity can store a reference to it
		hostEntity.save();
		
		InternetAddressEntity addressEntity = new InternetAddressEntity(workspace, hostEntity, address.toString());
		// Now save the address so that we can create a reference to it in the HostEntity
		addressEntity.save();
		addressEntity.addToSpace(spaceId);
		
		// It's now safe to assign the InternetAddressEntity 
		hostEntity.addAddress(addressEntity);
		hostEntity.save();
		hostEntity.addToSpace(spaceId);
		
		return addressEntity;
	}
}
