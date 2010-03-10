package com.netifera.platform.internal.model.events;

import com.netifera.platform.api.model.ISpace;
import com.netifera.platform.api.model.events.ISpaceChangeEvent;

public class SpaceChangeEvent extends SpaceEvent implements ISpaceChangeEvent {

	public SpaceChangeEvent(ISpace space) {
		super(space);
	}
}
