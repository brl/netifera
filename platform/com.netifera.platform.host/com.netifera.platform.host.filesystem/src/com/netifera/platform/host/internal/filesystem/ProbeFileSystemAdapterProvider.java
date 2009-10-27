package com.netifera.platform.host.internal.filesystem;

import java.net.URISyntaxException;

import com.netifera.platform.api.iterables.IndexedIterable;
import com.netifera.platform.api.model.IEntity;
import com.netifera.platform.api.model.IEntityAdapterProvider;
import com.netifera.platform.host.filesystem.FileSystemLocator;
import com.netifera.platform.model.ProbeEntity;

public class ProbeFileSystemAdapterProvider implements IEntityAdapterProvider {

	public Object getAdapter(IEntity entity, Class<?> adapterType) {
		if (adapterType.isAssignableFrom(FileSystemLocator.class)) {
			if (entity instanceof ProbeEntity) {
				try {
					return new FileSystemLocator("probe://"+((ProbeEntity)entity).getProbeId()+"/");
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
