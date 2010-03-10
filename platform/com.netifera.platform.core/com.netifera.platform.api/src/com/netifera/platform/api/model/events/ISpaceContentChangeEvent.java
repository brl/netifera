package com.netifera.platform.api.model.events;

import com.netifera.platform.api.model.IEntity;

public interface ISpaceContentChangeEvent extends ISpaceChangeEvent {
	IEntity getEntity();

	boolean isEntityAddEvent();
	boolean isEntityUpdateEvent();
	boolean isEntityRemoveEvent();
}