package com.netifera.platform.host.internal.terminal;

import java.net.URISyntaxException;

import com.netifera.platform.api.iterables.IndexedIterable;
import com.netifera.platform.api.model.IEntity;
import com.netifera.platform.api.model.IEntityAdapterProvider;
import com.netifera.platform.host.terminal.TerminalServiceLocator;
import com.netifera.platform.model.ProbeEntity;

public class ProbeTerminalAdapterProvider implements IEntityAdapterProvider {

	public Object getAdapter(IEntity entity, Class<?> adapterType) {
		if (adapterType.isAssignableFrom(TerminalServiceLocator.class)) {
			if (entity instanceof ProbeEntity) {
				try {
					return new TerminalServiceLocator("probe://"+((ProbeEntity)entity).getProbeId()+"/", ((ProbeEntity)entity).getHostEntity());
				} catch (URISyntaxException e) {
					throw new RuntimeException(e);
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
