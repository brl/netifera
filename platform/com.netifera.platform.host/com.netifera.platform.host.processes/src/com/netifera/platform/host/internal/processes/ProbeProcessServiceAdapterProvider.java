package com.netifera.platform.host.internal.processes;

import java.net.URISyntaxException;

import com.netifera.platform.api.iterables.IndexedIterable;
import com.netifera.platform.api.model.IEntity;
import com.netifera.platform.api.model.IEntityAdapterProvider;
import com.netifera.platform.host.processes.ProcessServiceLocator;
import com.netifera.platform.model.ProbeEntity;

public class ProbeProcessServiceAdapterProvider implements IEntityAdapterProvider {

	public Object getAdapter(IEntity entity, Class<?> adapterType) {
		if (adapterType.isAssignableFrom(ProcessServiceLocator.class)) {
			if (entity instanceof ProbeEntity) {
				try {
					return new ProcessServiceLocator("probe://"+((ProbeEntity)entity).getProbeId()+"/", ((ProbeEntity)entity).getHostEntity());
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
