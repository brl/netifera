package com.netifera.platform.internal.model.events;

import com.netifera.platform.api.model.ISpace;
import com.netifera.platform.api.model.events.ISpaceStatusChangeEvent;

public class SpaceStatusChangeEvent extends SpaceEvent implements ISpaceStatusChangeEvent {

	public SpaceStatusChangeEvent(ISpace space) {
		super(space);
	}

	public boolean isOpenEvent() {
		return getSpace().isOpened();
	}
	
	public boolean isCloseEvent() {
		return !isOpenEvent();
	}
}
