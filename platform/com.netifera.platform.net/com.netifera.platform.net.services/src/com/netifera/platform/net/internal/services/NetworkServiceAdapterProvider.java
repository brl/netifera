package com.netifera.platform.net.internal.services;

import java.util.HashMap;
import java.util.Map;

import com.netifera.platform.api.iterables.IndexedIterable;
import com.netifera.platform.api.model.IEntity;
import com.netifera.platform.api.model.IEntityAdapterProvider;
import com.netifera.platform.net.model.ClientServiceConnectionEntity;
import com.netifera.platform.net.model.PasswordEntity;
import com.netifera.platform.net.model.ServiceEntity;
import com.netifera.platform.net.model.UsernameAndPasswordEntity;
import com.netifera.platform.net.services.INetworkServiceProvider;
import com.netifera.platform.net.services.credentials.Password;
import com.netifera.platform.net.services.credentials.UsernameAndPassword;
import com.netifera.platform.util.addresses.inet.InternetSocketAddress;

public class NetworkServiceAdapterProvider implements IEntityAdapterProvider {
	private Map<String,INetworkServiceProvider> providers = new HashMap<String,INetworkServiceProvider>();
	
	public Object getAdapter(IEntity entity, Class<?> adapterType) {
		if (adapterType.isAssignableFrom(Password.class)) {
			if (entity instanceof PasswordEntity)
				return new Password(((PasswordEntity)entity).getPassword());
		} else if (adapterType.isAssignableFrom(UsernameAndPassword.class)) {
			if (!(entity instanceof UsernameAndPasswordEntity))
				return null;
			UsernameAndPasswordEntity c = (UsernameAndPasswordEntity)entity;
			return new UsernameAndPassword(c.getUsername(),c.getPassword());
		}

		ServiceEntity serviceEntity = null;
		if (entity instanceof ServiceEntity)
			serviceEntity = (ServiceEntity)entity;
		if (entity instanceof ClientServiceConnectionEntity)
			serviceEntity = ((ClientServiceConnectionEntity)entity).getService();
		
		if (serviceEntity == null)
			return null;
		
		InternetSocketAddress socketAddress = serviceEntity.toSocketAddress();
		if (adapterType.isAssignableFrom(socketAddress.getClass()))
			return socketAddress;

		String serviceType = serviceEntity.getServiceType();
		if (serviceType != null) {
			INetworkServiceProvider provider = providers.get(serviceType);
			if (provider != null && adapterType.isAssignableFrom(provider.getServiceClass())) {
				return provider.create(socketAddress);
			}
		}
		
		return null;
	}
		
	protected void registerProvider(INetworkServiceProvider provider) {
		providers.put(provider.getServiceName(), provider);
	}
	
	protected void unregisterProvider(INetworkServiceProvider provider) {
		providers.remove(provider.getServiceName()); // FIXME what if two providers with the same service name? should not happen
	}

	public IndexedIterable<?> getIterableAdapter(IEntity entity, Class<?> iterableType) {
		// TODO Auto-generated method stub
		return null;
	}
}
