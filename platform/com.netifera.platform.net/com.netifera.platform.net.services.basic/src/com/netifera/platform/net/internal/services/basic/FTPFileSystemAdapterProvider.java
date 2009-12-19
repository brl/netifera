package com.netifera.platform.net.internal.services.basic;

import java.net.URISyntaxException;

import com.netifera.platform.api.iterables.IndexedIterable;
import com.netifera.platform.api.model.IEntity;
import com.netifera.platform.api.model.IEntityAdapterProvider;
import com.netifera.platform.host.filesystem.FileSystemServiceLocator;
import com.netifera.platform.net.model.ServiceEntity;
import com.netifera.platform.net.model.UsernameAndPasswordEntity;
import com.netifera.platform.net.services.basic.FTP;

public class FTPFileSystemAdapterProvider implements IEntityAdapterProvider {

	public Object getAdapter(IEntity entity, Class<?> adapterType) {
		if (adapterType.isAssignableFrom(FileSystemServiceLocator.class)) {
			if (entity instanceof UsernameAndPasswordEntity) {
				UsernameAndPasswordEntity credentialEntity = (UsernameAndPasswordEntity) entity;
				
				if (credentialEntity.getAuthenticable() instanceof ServiceEntity) {
					ServiceEntity serviceEntity = (ServiceEntity) credentialEntity.getAuthenticable();
					FTP ftp = (FTP) credentialEntity.getAuthenticable().getAdapter(FTP.class);
					if (ftp != null) {
						try {
							return new FileSystemServiceLocator("ftp://"+credentialEntity.getUsername()+":"+credentialEntity.getPassword()+"@"+ftp.getSocketAddress().getNetworkAddress()+":"+ftp.getSocketAddress().getPort()+"/", serviceEntity.getAddress().getHost());
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
