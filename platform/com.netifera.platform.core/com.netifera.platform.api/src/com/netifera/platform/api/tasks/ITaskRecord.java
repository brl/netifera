package com.netifera.platform.api.tasks;

import java.util.List;

import com.netifera.platform.api.events.IEventHandler;

public interface ITaskRecord extends ITaskStatus {
	long getTaskId();
	long getProbeId();

	List<ITaskOutput> getTaskOutput();
	void addTaskOutput(ITaskOutput output);

	void addTaskOutputListener(IEventHandler handler);
	void removeTaskOutputListener(IEventHandler handler);
}
