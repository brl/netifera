package com.netifera.platform.internal.model;

import java.io.Serializable;

import com.netifera.platform.api.model.IEntity;
import com.netifera.platform.api.model.IEntityReference;
import com.netifera.platform.api.model.IWorkspace;

public class EntityReference implements IEntityReference, Serializable {

	private static final long serialVersionUID = 454593980781826497L;
	
	private final long entityId;
	private transient volatile IEntity cache;
	
	public EntityReference(IEntity entity) {
		this.entityId = entity.getId();
		this.cache = entity;
	}

	public EntityReference(long id) {
		if(id <= 0) {
			throw new IllegalArgumentException();
		}
		this.entityId = id;
		this.cache = null;
	}
	
	public IEntity getEntity(IWorkspace workspace) {
		if(cache == null) {
			cache = workspace.findById(entityId);
		}
		return cache;
	}

	public void setEntity(IEntity entity) {
		cache = entity;
	}
	
	public void freeCache() {
		cache = null;
	}
	
	public long getId() {
		return entityId;
	}
	
	public int hashCode() {
		return (int) entityId;
	}
	
	public boolean equals(Object o) {
		return (o instanceof EntityReference) && entityId == ((EntityReference)o).entityId;
	}
}
