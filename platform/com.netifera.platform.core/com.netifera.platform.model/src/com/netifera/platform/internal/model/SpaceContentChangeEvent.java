
package com.netifera.platform.internal.model;

import com.netifera.platform.api.model.IEntity;
import com.netifera.platform.api.model.ISpaceContentChangeEvent;

class SpaceContentChangeEvent implements ISpaceContentChangeEvent {
	
	private final boolean isUpdate;
	private final boolean isAdd;
	private final boolean isRemove;
	private final IEntity entity;

	public static SpaceContentChangeEvent createUpdateEvent(IEntity entity) {
		return new SpaceContentChangeEvent(entity, true, false, false);
	}
	
	public static SpaceContentChangeEvent createAdditionEvent(IEntity entity) {
		return new SpaceContentChangeEvent(entity, false, true, false);
	}

	public static SpaceContentChangeEvent createRemovalEvent(IEntity entity) {
		return new SpaceContentChangeEvent(entity, false, false, true);
	}

	private SpaceContentChangeEvent(IEntity entity, boolean update, boolean add, boolean remove) {
		this.entity = entity;
		this.isUpdate = update;
		this.isAdd = add;
		this.isRemove = remove;
	}
	
	public IEntity getEntity() {
		return entity;
	}
	
	public boolean isAdditionEvent() {
		return isAdd;
	}
	
	public boolean isUpdateEvent() {
		return isUpdate;
	}
	
	public boolean isRemovalEvent() {
		return isRemove;
	}
}