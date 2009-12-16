package com.netifera.platform.internal.model.events;

import com.netifera.platform.api.model.ISpace;
import com.netifera.platform.api.model.events.ISpaceEvent;

public abstract class SpaceEvent implements ISpaceEvent {

	final private ISpace space;
	
	public SpaceEvent(ISpace space) {
		this.space = space;
	}
	
	public ISpace getSpace() {
		return space;
	}
}
