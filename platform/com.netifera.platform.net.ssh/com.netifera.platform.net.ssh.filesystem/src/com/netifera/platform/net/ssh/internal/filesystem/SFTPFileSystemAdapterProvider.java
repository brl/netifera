package com.netifera.platform.net.ssh.internal.filesystem;

import java.net.URISyntaxException;

import com.netifera.platform.api.iterables.IndexedIterable;
import com.netifera.platform.api.model.IEntity;
import com.netifera.platform.api.model.IEntityAdapterProvider;
import com.netifera.platform.host.filesystem.FileSystemLocator;
import com.netifera.platform.net.model.UsernameAndPasswordEntity;
import com.netifera.platform.net.services.ssh.SSH;

public class SFTPFileSystemAdapterProvider implements IEntityAdapterProvider {

	public Object getAdapter(IEntity entity, Class<?> adapterType) {
		if (adapterType.isAssignableFrom(FileSystemLocator.class)) {
			if (entity instanceof UsernameAndPasswordEntity) {
				UsernameAndPasswordEntity credentialEntity = (UsernameAndPasswordEntity) entity;
				
				SSH ssh = (SSH) credentialEntity.getAuthenticable().getAdapter(SSH.class);
				if (ssh != null) {
					try {
						return new FileSystemLocator("sftp://"+credentialEntity.getUsername()+":"+credentialEntity.getPassword()+"@"+ssh.getLocator().getAddress()+":"+ssh.getLocator().getPort()+"/");
					} catch (URISyntaxException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
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
