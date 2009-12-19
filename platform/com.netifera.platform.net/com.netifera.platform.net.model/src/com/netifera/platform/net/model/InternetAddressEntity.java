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
	
	private InternetAddressEntity(IWorkspace workspace, HostEntity host, byte[] address) {
		super(ENTITY_NAME, workspace, host.getRealmId(), address);
		this.host = host.createReference();
	}

	public InternetAddressEntity(IWorkspace workspace, HostEntity host, String address) {
		this(workspace, host, InternetAddress.fromString(address).toBytes());
	}

	private InternetAddressEntity(IWorkspace workspace, IEntityReference hostReference, long realmId, byte[] address) {
		super(ENTITY_NAME, workspace, realmId, address);
		this.host = hostReference == null ? null : hostReference.createClone();
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
}
