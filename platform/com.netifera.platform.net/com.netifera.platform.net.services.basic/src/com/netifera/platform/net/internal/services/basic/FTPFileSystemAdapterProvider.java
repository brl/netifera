package com.netifera.platform.net.internal.services.basic;

import java.net.URISyntaxException;

import com.netifera.platform.api.iterables.IndexedIterable;
import com.netifera.platform.api.model.IEntity;
import com.netifera.platform.api.model.IEntityAdapterProvider;
import com.netifera.platform.host.filesystem.FileSystemLocator;
import com.netifera.platform.net.model.UsernameAndPasswordEntity;
import com.netifera.platform.net.services.basic.FTP;

public class FTPFileSystemAdapterProvider implements IEntityAdapterProvider {

	public Object getAdapter(IEntity entity, Class<?> adapterType) {
		if (adapterType.isAssignableFrom(FileSystemLocator.class)) {
			if (entity instanceof UsernameAndPasswordEntity) {
				UsernameAndPasswordEntity credentialEntity = (UsernameAndPasswordEntity) entity;
				
				FTP ftp = (FTP) credentialEntity.getAuthenticable().getAdapter(FTP.class);
				if (ftp != null) {
					try {
						return new FileSystemLocator("ftp://"+credentialEntity.getUsername()+":"+credentialEntity.getPassword()+"@"+ftp.getLocator().getAddress()+":"+ftp.getLocator().getPort()+"/");
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
