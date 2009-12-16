package com.netifera.platform.api.model.events;

import com.netifera.platform.api.events.IEvent;
import com.netifera.platform.api.model.ISpace;

public interface ISpaceEvent extends IEvent {
	ISpace getSpace();
}
