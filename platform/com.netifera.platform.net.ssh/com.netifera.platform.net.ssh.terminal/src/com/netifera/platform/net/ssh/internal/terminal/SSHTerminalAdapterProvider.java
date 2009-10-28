package com.netifera.platform.net.ssh.internal.terminal;

import java.net.URISyntaxException;

import com.netifera.platform.api.iterables.IndexedIterable;
import com.netifera.platform.api.model.IEntity;
import com.netifera.platform.api.model.IEntityAdapterProvider;
import com.netifera.platform.host.terminal.TerminalServiceLocator;
import com.netifera.platform.net.model.ServiceEntity;
import com.netifera.platform.net.model.UsernameAndPasswordEntity;
import com.netifera.platform.net.services.ssh.SSH;

public class SSHTerminalAdapterProvider implements IEntityAdapterProvider {

	public Object getAdapter(IEntity entity, Class<?> adapterType) {
		if (adapterType.isAssignableFrom(TerminalServiceLocator.class)) {
			if (entity instanceof UsernameAndPasswordEntity) {
				UsernameAndPasswordEntity credentialEntity = (UsernameAndPasswordEntity) entity;
				
				if (credentialEntity.getAuthenticable() instanceof ServiceEntity) {
					ServiceEntity serviceEntity = (ServiceEntity) credentialEntity.getAuthenticable();
					SSH ssh = (SSH) credentialEntity.getAuthenticable().getAdapter(SSH.class);
					if (ssh != null) {
						try {
							return new TerminalServiceLocator("ssh://"+credentialEntity.getUsername()+":"+credentialEntity.getPassword()+"@"+ssh.getLocator().getAddress()+":"+ssh.getLocator().getPort()+"/", serviceEntity.getAddress().getHost());
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
