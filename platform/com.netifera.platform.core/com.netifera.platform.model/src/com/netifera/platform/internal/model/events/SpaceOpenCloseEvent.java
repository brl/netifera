package com.netifera.platform.internal.model.events;

import com.netifera.platform.api.model.ISpace;
import com.netifera.platform.api.model.events.ISpaceStatusChangeEvent;

public class SpaceOpenCloseEvent extends SpaceEvent implements ISpaceStatusChangeEvent {

	private final boolean opened;
	
	public SpaceOpenCloseEvent(ISpace space, boolean opened) {
		super(space);
		this.opened = opened;
	}

	public boolean isOpenEvent() {
		return opened;
	}
	
	public boolean isCloseEvent() {
		return !isOpenEvent();
	}

	public boolean isActivateEvent() {
		return false;
	}

	public boolean isDeactivateEvent() {
		return false;
	}
}
