package com.netifera.platform.api.model;

import com.netifera.platform.api.iterables.IndexedIterable;

public interface IEntityAdapterProvider {
	Object getAdapter(IEntity entity, Class<?> adapterType);
	IndexedIterable<?> getIterableAdapter(IEntity entity, Class<?> iterableType);
}
