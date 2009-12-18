package com.netifera.platform.tools.internal;

import com.netifera.platform.api.log.ILogger;
import com.netifera.platform.api.tasks.ITask;
import com.netifera.platform.api.tasks.ITaskMessenger;
import com.netifera.platform.api.tasks.ITaskOutput;
import com.netifera.platform.api.tasks.ITaskRunnable;
import com.netifera.platform.api.tasks.TaskException;
import com.netifera.platform.api.tools.ITool;
import com.netifera.platform.api.tools.IToolConfiguration;
import com.netifera.platform.api.tools.IToolContext;

public class ToolContext implements IToolContext, ITaskRunnable, ITaskMessenger {
	final private ITool tool;
	final private IToolConfiguration configuration;
	final private long realm;
	final private long spaceId;
	
	private ITask task;
	private boolean debugEnabled;
	
	ToolContext(ITool tool, IToolConfiguration configuration, long realm, long spaceId) {
		this.tool = tool;
		this.configuration = configuration;
		this.realm = realm;
		this.spaceId = spaceId;
		this.debugEnabled = false;
	}
	
	public void setTitle(String title) {
		task.setTitle(title);
	}

	public void setSubTitle(String subtitle) {
		task.setSubTitle(subtitle);
	}

	public long getRealm() {
		return realm;
	}
	
	public long getSpaceId() {
		return spaceId;
	}
	
	public void run(ITask task) throws TaskException {
		this.task = task;
		tool.run(this);
	}

	public IToolConfiguration getConfiguration() {
		return configuration;
	}

	public void addMessage(ITaskOutput message) {
		ITaskMessenger messenger = (ITaskMessenger) task;
		messenger.addMessage(message);		
	}

	public void enableDebugOutput() {
		debugEnabled = true;
	}
	
	public void debug(String message) {
		if(debugEnabled) {
			task.debug(message);
		}
	}

	public void setTotalWork(int totalWork) {
		task.setTotalWork(totalWork);
	}

	public void worked(int work) {
		task.worked(work);		
	}

	public void done() {
		task.done();		
	}

	public void error(String message) {
		task.error(message);
	}

	public void info(String message) {
		task.info(message);
	}

	public void print(String message) {
		task.print(message);
	}
	
	public void exception(String message, Throwable throwable) {
		task.exception(message, throwable);
	}
	
	public void warning(String message) {
		task.warning(message);
	}

	public ILogger getLogger() {
		return new ToolLogger(this);
	}
}
