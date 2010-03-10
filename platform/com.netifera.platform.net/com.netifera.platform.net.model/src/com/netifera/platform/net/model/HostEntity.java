package com.netifera.platform.net.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.netifera.platform.api.model.AbstractEntity;
import com.netifera.platform.api.model.IEntity;
import com.netifera.platform.api.model.IEntityReference;
import com.netifera.platform.api.model.IWorkspace;

public class HostEntity extends AbstractEntity {
	
	private static final long serialVersionUID = 7428760363871082973L;

	public final static String ENTITY_NAME = "host";

	private String label;
	
	private List<IEntityReference> addresses = new ArrayList<IEntityReference>();
	private IEntityReference defaultAddress;
	
	public HostEntity(IWorkspace workspace, long realmId) {
		super(ENTITY_NAME, workspace, realmId);	
	}
	
	HostEntity() {}
	
	public void addAddress(NetworkAddressEntity address) {
		if (addresses.size() == 0)
			setDefaultAddress(address);
		addresses.add(address.createReference());
	}

	public List<NetworkAddressEntity> getAddresses() {
		List<NetworkAddressEntity> answer = new ArrayList<NetworkAddressEntity>();
		for (IEntityReference ref: addresses)
			answer.add((NetworkAddressEntity) referenceToEntity(ref));
		return answer;
	}
	
	public void setDefaultAddress(NetworkAddressEntity address) {
		this.defaultAddress = address.createReference();
	}
	
	public NetworkAddressEntity getDefaultAddress() {
		return (NetworkAddressEntity) referenceToEntity(defaultAddress);
	}
		
	public void setLabel(String label) {
		this.label = label;
	}
	
	public String getLabel() {
		return label;
	}

	public boolean hasAddress(byte[] address) {
		if (Arrays.equals(getDefaultAddress().getData(), address))
			return true;
		for (IEntityReference ref: addresses) {
			NetworkAddressEntity addressEntity = (NetworkAddressEntity) referenceToEntity(ref);
			if (Arrays.equals(addressEntity.getData(), address))
				return true;
		}
		return false;
	}
	
	public String getPlatform() {
		String os = getAttribute("os");
		String arch = getAttribute("arch");
		String platformOS = null;
		String platformArch = null;
		
		if (os == null)
			return null;

		if (os.toLowerCase().matches(".*linux.*")) {
			platformOS = "linux";
		} else if (os.toLowerCase().matches(".*windows.*")) {
			platformOS = "windows";
			platformArch = "i386";
		} else if (os.toLowerCase().matches(".*osx.*")) {
			platformOS = "osx";
			platformArch = "i386";
		}

		if (arch.toLowerCase().matches(".*(i.86|x86|intel).*")) {
			platformArch = "i386";
		} else if (arch.toLowerCase().matches("(x64|amd64)")) {
			platformArch = "x64";
		}
		
		if (platformOS != null && platformArch != null) {
			return platformOS+"/"+platformArch;
		}
		return null;
	}
	
	@Override
	protected void synchronizeEntity(AbstractEntity masterEntity) {
		label = ((HostEntity)masterEntity).label;
	}
	
	protected IEntity cloneEntity() {
		HostEntity clone = new HostEntity(getWorkspace(), getRealmId());
		
		clone.label = label;

		for (IEntityReference ref: addresses)
			clone.addresses.add(ref);
		clone.defaultAddress = defaultAddress;
		
		return clone;
	}
}
