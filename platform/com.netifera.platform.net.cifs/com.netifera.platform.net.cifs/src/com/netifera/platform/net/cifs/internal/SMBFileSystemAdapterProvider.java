package com.netifera.platform.net.cifs.internal;

import java.net.URISyntaxException;

import com.netifera.platform.api.iterables.IndexedIterable;
import com.netifera.platform.api.model.IEntity;
import com.netifera.platform.api.model.IEntityAdapterProvider;
import com.netifera.platform.host.filesystem.FileSystemServiceLocator;
import com.netifera.platform.net.model.ServiceEntity;
import com.netifera.platform.net.model.UsernameAndPasswordEntity;
import com.netifera.platform.util.addresses.inet.TCPSocketAddress;

public class SMBFileSystemAdapterProvider implements IEntityAdapterProvider {

	public Object getAdapter(IEntity entity, Class<?> adapterType) {
		if (adapterType.isAssignableFrom(FileSystemServiceLocator.class)) {
			if (entity instanceof UsernameAndPasswordEntity) {
				UsernameAndPasswordEntity credentialEntity = (UsernameAndPasswordEntity) entity;
				
				IEntity authenticableEntity = credentialEntity.getAuthenticable();
				if (authenticableEntity instanceof ServiceEntity) {
					ServiceEntity serviceEntity = (ServiceEntity) authenticableEntity;
					TCPSocketAddress socketAddress = (TCPSocketAddress) serviceEntity.getAdapter(TCPSocketAddress.class);
					if (socketAddress != null && "NetBIOS-SSN".equals(serviceEntity.getServiceType())) {
						try {
							return new FileSystemServiceLocator("smb://"+credentialEntity.getUsername()+":"+credentialEntity.getPassword()+"@"+socketAddress.getNetworkAddress()+":"+socketAddress.getPort()+"/", serviceEntity.getAddress().getHost());
						} catch (URISyntaxException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}
		}
		
		return null;
	}

	public IndexedIterable<?> getIterableAdapter(IEntity entity,
			Class<?> iterableType) {
		// TODO Auto-generated method stub
		return null;
	}
}
