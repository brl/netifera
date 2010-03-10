package com.netifera.platform.internal.model.events;

import com.netifera.platform.api.model.ISpace;
import com.netifera.platform.api.model.events.ISpaceRenameEvent;

public class SpaceRenameEvent extends SpaceChangeEvent implements ISpaceRenameEvent {

	public SpaceRenameEvent(ISpace space) {
		super(space);
	}

	public String getName() {
		return getSpace().getName();
	}
}
