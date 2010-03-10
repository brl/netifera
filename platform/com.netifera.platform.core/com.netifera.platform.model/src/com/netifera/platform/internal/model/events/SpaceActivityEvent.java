package com.netifera.platform.internal.model.events;

import com.netifera.platform.api.model.ISpace;
import com.netifera.platform.api.model.events.ISpaceStatusChangeEvent;

public class SpaceActivityEvent extends SpaceEvent implements ISpaceStatusChangeEvent {

	private final boolean active;
	
	public SpaceActivityEvent(ISpace space, boolean active) {
		super(space);
		this.active = active;
	}

	public boolean isOpenEvent() {
		return false;
	}
	
	public boolean isCloseEvent() {
		return false;
	}

	public boolean isActivateEvent() {
		return active;
	}

	public boolean isDeactivateEvent() {
		return !isActivateEvent();
	}
}
