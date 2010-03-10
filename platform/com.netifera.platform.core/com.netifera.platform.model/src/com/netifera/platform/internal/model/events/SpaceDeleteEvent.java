package com.netifera.platform.internal.model.events;

import com.netifera.platform.api.model.ISpace;
import com.netifera.platform.api.model.events.ISpaceLifecycleEvent;

public class SpaceDeleteEvent extends SpaceEvent implements ISpaceLifecycleEvent {

	public SpaceDeleteEvent(ISpace space) {
		super(space);
	}

	public boolean isDeleteEvent() {
		return true;
	}

	public boolean isCreateEvent() {
		return false;
	}
}
