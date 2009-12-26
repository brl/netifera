package com.netifera.platform.internal.model;

import java.io.Serializable;

import com.netifera.platform.api.model.IEntity;
import com.netifera.platform.api.model.IEntityReference;
import com.netifera.platform.api.model.IWorkspace;

public class EntityReference implements IEntityReference, Serializable {

	private static final long serialVersionUID = 454593980781826497L;
	
	private final long entityId;
	transient private IEntity cachedEntity;
	
	public EntityReference(IEntity entity) {
		this.entityId = entity.getId();
		this.cachedEntity = entity;
	}

	public EntityReference(long id) {
		if(id <= 0) {
			throw new IllegalArgumentException();
		}
		this.entityId = id;
		this.cachedEntity = null;
	}
	
	public IEntityReference createClone() {
		return this; //new EntityReference(entityId);
	}

	public IEntity getEntity(IWorkspace workspace) {
		if(cachedEntity == null) {
			cachedEntity = workspace.findById(entityId);
		}
		return cachedEntity;
	}

	public void freeCachedEntity() {
		cachedEntity = null;
	}
	
	public long getId() {
		return entityId;
	}
	
	public int hashCode() {
		return (int) entityId;
	}
	
	public boolean equals(Object o) {
		return (o instanceof EntityReference) && entityId == ((EntityReference)o).getId();
	}
}
