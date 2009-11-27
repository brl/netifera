package com.netifera.platform.model;

import com.netifera.platform.api.model.AbstractEntity;
import com.netifera.platform.api.model.IEntity;
import com.netifera.platform.api.model.IWorkspace;

public class SpaceEntity extends AbstractEntity {
	
	private static final long serialVersionUID = -646257324642300345L;
	public final static String ENTITY_NAME = "space";
	
	private /*final*/ long spaceId = -1;
	
	public SpaceEntity(IWorkspace workspace, IEntity realmEntity) {
		super(ENTITY_NAME, workspace, realmEntity.getId());
	}
	
	private SpaceEntity(IWorkspace workspace, long realmId) {
		super(ENTITY_NAME, workspace, realmId);
	}

	public void setSpaceId(long spaceId) {
		this.spaceId = spaceId;
	}
	
	public long getSpaceId() {
		return spaceId;
	}
	
	public boolean isRealmEntity() {
		return true;
	}

	protected IEntity cloneEntity() {
		SpaceEntity clone = new SpaceEntity(getWorkspace(), getRealmId());
		clone.setSpaceId(spaceId);
		return clone;
	}
}
