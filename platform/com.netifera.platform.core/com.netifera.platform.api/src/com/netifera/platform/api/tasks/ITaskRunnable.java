package com.netifera.platform.api.tasks;

public interface ITaskRunnable {
	void run(ITask task) throws TaskException;
}
