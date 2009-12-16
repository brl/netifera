package com.netifera.platform.api.model.events;

import com.netifera.platform.api.events.IEvent;
import com.netifera.platform.api.tasks.ITaskOutput;
import com.netifera.platform.api.tasks.ITaskRecord;

public interface ISpaceTaskChangeEvent extends IEvent {
	ITaskRecord getTask();
	ITaskOutput getOutput();
	
	boolean isCreateEvent();
	boolean isUpdateEvent();
	boolean isOutputEvent();
}
