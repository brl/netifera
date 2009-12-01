package com.netifera.platform.api.model;

import com.netifera.platform.api.events.IEvent;

public interface ISpaceContentChangeEvent extends IEvent {
	IEntity getEntity();

	boolean isAdditionEvent();
	boolean isUpdateEvent();
	boolean isRemovalEvent();
}