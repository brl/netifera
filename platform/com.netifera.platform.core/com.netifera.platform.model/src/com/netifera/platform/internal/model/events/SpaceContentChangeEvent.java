
package com.netifera.platform.internal.model.events;

import com.netifera.platform.api.model.IEntity;
import com.netifera.platform.api.model.ISpace;
import com.netifera.platform.api.model.events.ISpaceContentChangeEvent;

public class SpaceContentChangeEvent extends SpaceChangeEvent implements ISpaceContentChangeEvent {
	
	private final boolean isUpdate;
	private final boolean isAdd;
	private final boolean isRemove;
	private final IEntity entity;

	public static SpaceContentChangeEvent createUpdateEvent(ISpace space, IEntity entity) {
		return new SpaceContentChangeEvent(space, entity, true, false, false);
	}
	
	public static SpaceContentChangeEvent createAddEvent(ISpace space, IEntity entity) {
		return new SpaceContentChangeEvent(space, entity, false, true, false);
	}

	public static SpaceContentChangeEvent createRemoveEvent(ISpace space, IEntity entity) {
		return new SpaceContentChangeEvent(space, entity, false, false, true);
	}

	private SpaceContentChangeEvent(ISpace space, IEntity entity, boolean update, boolean add, boolean remove) {
		super(space);
		this.entity = entity;
		this.isUpdate = update;
		this.isAdd = add;
		this.isRemove = remove;
	}
	
	public IEntity getEntity() {
		return entity;
	}
	
	public boolean isEntityAddEvent() {
		return isAdd;
	}
	
	public boolean isEntityUpdateEvent() {
		return isUpdate;
	}
	
	public boolean isEntityRemoveEvent() {
		return isRemove;
	}
}