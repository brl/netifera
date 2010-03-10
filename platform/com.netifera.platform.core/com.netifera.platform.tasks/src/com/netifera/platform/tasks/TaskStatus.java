package com.netifera.platform.tasks;

import java.io.Serializable;

import com.netifera.platform.api.tasks.ITaskStatus;

public class TaskStatus implements Serializable, ITaskStatus {
    private static final long serialVersionUID = 5386061977451709956L;
    
    public final static int WAITING =  0;
    public final static int RUNNING =  1;
    public final static int FINISHED = 2;
    public final static int FAILED =   3;

	final private long taskId;
	
	private volatile String title;
	private volatile String subtitle;
	
	private volatile int runState;
	
	private volatile long startTime;
	private volatile int workDone;
	private volatile long elapsedTime;
	
	public TaskStatus(long taskId) {
		this.taskId = taskId;
		runState = WAITING;
		workDone = -1;
	}

	public long getTaskId() {
		return taskId;
	}
	
	public void update(ITaskStatus newStatus) {
		this.title = newStatus.getTitle();
		this.subtitle = newStatus.getSubTitle();
		this.runState = newStatus.getRunState();
		this.startTime = newStatus.getStartTime();
		this.workDone = newStatus.getWorkDone();
		this.elapsedTime = newStatus.getElapsedTime();
	}
	
	public void setTitle(String title) {
		this.title = title;
	}
	
	public void setSubTitle(String subtitle) {
		this.subtitle = subtitle;
	}
	
	public String getTitle() {
		return title;
	}
	
	public String getSubTitle() {
		return subtitle;
	}

	public int getRunState() {
		return runState;
	}
	
	public void setRunState(int newState) {
		runState = newState;
	}
	
	public void updateElapsedTime() {
		if(startTime != 0) {
			elapsedTime = System.currentTimeMillis() - startTime;
		}
	}
	
	public void setStartTime(long time) {
		startTime = time;
	}
	
	public long getStartTime() {
		return startTime;
	}
	
	public long getElapsedTime() {
		return elapsedTime;
	}

	public int getWorkDone() {
		return workDone;
	}

	public void setWorkDone(int workDone) {
		this.workDone = workDone;
	}	
	
    public String getStateDescription() {
        switch (getRunState()) {
        case WAITING:
            return "Waiting";
        case RUNNING:
            return "Running";
        case FINISHED:
            return "Finished";
        case FAILED:
            return "Failed";
        default:
            return "??";
        }
    }

    @Override
	public String toString() {
		return "TaskRecord: taskId=" + getTaskId() +  " '" +  getTitle() + 
			"'  [" + getStateDescription() + "] " + workToString();
	}
    
    private String workToString() {
    	if(getWorkDone() == -1) {
    		return "";
    	} else {
    		return "(" + workDone + "%)";
    	}
    }
    
	@Override
	public boolean equals(Object o) {
		if(!(o instanceof TaskStatus)) 
			return false;
		
		if(this == o) 
			return true;
		
		final TaskStatus other = (TaskStatus)o;
		return (taskId == other.taskId);
	}
	
	@Override
	public int hashCode() {
		return (int) (taskId ^ (taskId >> 32));
	}

    public void setFailed() {
        runState = FAILED;
    }

    public void setFinished() {
        runState = FINISHED;
    }

    public void setRunning() {
        runState = RUNNING;
    }

    public void setWaiting() {
        runState = WAITING;
    }

    public boolean isFailed() {
        return runState == FAILED;
    }

    public boolean isFinished() {
        return runState == FINISHED;
    }

    public boolean isRunning() {
        return runState == RUNNING;
    }

    public boolean isWaiting() {
        return runState == WAITING;
    }
}
