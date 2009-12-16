package com.netifera.platform.internal.model.events;

import com.netifera.platform.api.model.ISpace;
import com.netifera.platform.api.model.events.ISpaceLifecycleEvent;

public class SpaceCreateEvent extends SpaceEvent implements ISpaceLifecycleEvent {

	public SpaceCreateEvent(ISpace space) {
		super(space);
	}

	public boolean isDeleteEvent() {
		return false;
	}

	public boolean isCreateEvent() {
		return true;
	}
}
