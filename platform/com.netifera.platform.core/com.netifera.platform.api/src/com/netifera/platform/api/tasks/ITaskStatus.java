package com.netifera.platform.api.tasks;

public interface ITaskStatus {
	long getTaskId();

	String getTitle();
	String getSubTitle();

	int getRunState();
    String getStateDescription();
    boolean isRunning();
    boolean isWaiting();
    boolean isFinished();
    boolean isFailed();

	long getStartTime();
	long getElapsedTime();

	int getWorkDone();
	
	void update(ITaskStatus newStatus);
}
