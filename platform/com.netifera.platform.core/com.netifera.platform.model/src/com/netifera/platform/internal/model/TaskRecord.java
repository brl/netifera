package com.netifera.platform.internal.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.db4o.ext.DatabaseClosedException;
import com.netifera.platform.api.events.EventListenerManager;
import com.netifera.platform.api.events.IEventHandler;
import com.netifera.platform.api.tasks.ITaskOutput;
import com.netifera.platform.api.tasks.ITaskOutputEvent;
import com.netifera.platform.api.tasks.ITaskRecord;
import com.netifera.platform.api.tasks.ITaskStatus;

public class TaskRecord implements ITaskRecord {
	private final static int BACKGROUND_COMMIT_INTERVAL = 30000;
	
	private final long taskId;
	private final Space space;
	private final ITaskStatus status;
	private final List<ITaskOutput> outputs;
	private transient EventListenerManager taskChangeListeners;
	
	private transient Thread commitThread;
	private transient volatile boolean commitThreadActive;
	private transient volatile boolean taskOutputDirty;
	
	TaskRecord(ITaskStatus status, Space space) {
		this.space = space;
		this.status = status;
		this.outputs = new ArrayList<ITaskOutput>();
		this.taskId = status.getTaskId();
		this.commitThreadActive = false;
	}

	public void update(ITaskStatus newStatus) {
		status.update(newStatus);
		space.getDatabase().store(status);
		space.updateTask(this);
		if(status.isFinished() || status.isFailed()) {
			stopCommitThread();
		}
	}
	
	public void addTaskOutputListener(IEventHandler handler) {
		getEventManager().addListener(handler);
	}
	
	public void removeTaskOutputListener(IEventHandler handler) {
		getEventManager().removeListener(handler);
	}
	
	private EventListenerManager getEventManager() {
		if(taskChangeListeners == null) {
			taskChangeListeners = new EventListenerManager();
		}
		return taskChangeListeners;
	}
	
	public List<ITaskOutput> getTaskOutput() {
		return Collections.unmodifiableList(outputs);
	}
	
	public void addTaskOutput(final ITaskOutput output) {
		synchronized(outputs) {
			outputs.add(output);
			taskOutputDirty = true;
			startCommitThread();
		}
		space.updateTask(this);
		getEventManager().fireEvent(new ITaskOutputEvent() {
			public ITaskOutput getMessage() {
				return output;
			}
		});
	}

	public long getProbeId() {
		return space.getProbeId();
	}

	/* Must not delegate to taskStatus or it will break the query optimizer */
	public long getTaskId() {
		return taskId;
	}

	public ITaskStatus getStatus() {
		return status;
	}
	
	private synchronized void startCommitThread() {
		if(commitThreadActive) {
			return;
		}
		commitThread = new Thread(new Runnable() {
			public void run() {
				while(commitThreadActive) {
					try {
						Thread.sleep(BACKGROUND_COMMIT_INTERVAL);
						if(space.getDatabase().ext().isClosed()) {
							commitThreadActive = false;
							return;
						} else {
							runCommit();
						}
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						commitThreadActive = false;
					} catch(DatabaseClosedException e) {
						commitThreadActive = false;
						return;
					}
				}
				runCommit();
				return;	
			}
		});
		commitThread.setDaemon(true);
		if(status.getTitle() != null) {
			commitThread.setName("Background Commit TaskRecord [" + status.getTitle() + "]");
		} else {
			commitThread.setName("Background Commit TaskRecord [taskId = " + taskId + "]");
		}
		commitThreadActive = true;
		commitThread.start();
	}
	
	private synchronized void stopCommitThread() {
		if(!commitThreadActive) {
			return;
		}
		commitThreadActive = false;
		commitThread.interrupt();
//		try {
//			commitThread.join();
//		} catch (InterruptedException e) {
//			Thread.currentThread().interrupt();
//		}
		commitThread = null;
	}
	
	private void runCommit() {
		synchronized(outputs) {
			if(!taskOutputDirty) 
				return;
			space.getDatabase().store(outputs);
		}
	}

	/*
	 * Delegate to the internal ITaskStatus
	 */
	
	public long getElapsedTime() {
		return status.getElapsedTime();
	}

	public int getRunState() {
		return status.getRunState();
	}

	public long getStartTime() {
		return status.getStartTime();
	}

	public String getStateDescription() {
		return status.getStateDescription();
	}

	public String getSubTitle() {
		return status.getSubTitle();
	}

	public String getTitle() {
		return status.getTitle();
	}

	public int getWorkDone() {
		return status.getWorkDone();
	}

	public boolean isFailed() {
		return status.isFailed();
	}

	public boolean isFinished() {
		return status.isFinished();
	}

	public boolean isRunning() {
		return status.isRunning();
	}

	public boolean isWaiting() {
		return status.isWaiting();
	}
}
