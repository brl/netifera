package com.netifera.platform.tools;

import com.netifera.platform.api.dispatcher.ProbeMessage;
import com.netifera.platform.api.tools.IToolConfiguration;

public class ToolLaunchMessage extends ProbeMessage {
	
	private static final long serialVersionUID = -6356480222400163080L;

	public final static String ID = "ToolLaunch";

	String taskTitle;
	String className;
	IToolConfiguration configuration;
	long realm;
	long spaceId;
	long taskId;
	
	public ToolLaunchMessage(String className, IToolConfiguration configuration, long realm, long spaceId) {
		this(null, className, configuration, realm, spaceId);
	}
	
	public ToolLaunchMessage(String taskTitle, String className, IToolConfiguration configuration, long realm, long spaceId) {
		super(ID);
		this.taskTitle = taskTitle;
		this.className = className;
		this.configuration = configuration;
		this.realm = realm;
		this.spaceId = spaceId;
	}
	
/*	public ToolLaunchMessage(String taskTitle, String className, IToolConfiguration configuration) {
		this(taskTitle, className, configuration, -1, 0);
	}
*/	
	public ToolLaunchMessage(long taskId, int sequence) {
		super(ID);
		this.taskId = taskId;
		setSequenceNumber(sequence);
		markAsResponse();
	}
	
	public ToolLaunchMessage createResponse(long taskId) {
		return new ToolLaunchMessage(taskId, getSequenceNumber());
	}
	
	public long getTaskId() {
		return taskId;
	}

	public long getRealm() {
		return realm;
	}

	public long getSpaceId() {
		return spaceId;
	}
	
	@Override
	public String toString() {
		if(isResponse()) {
			return super.toString() + " taskId = " + taskId;
		} else {
		return super.toString()
			+ (taskTitle != null ? " task:\"" + taskTitle + "\", ": "")
			+ " class:" + className
			+ ", options:" + configuration.getOptions();
		}
	}
	
	public IToolConfiguration getConfiguration() {
		return configuration;
	}

	public String getClassName() {
		return className;
	}
	
	public String getTaskTitle() {
		return taskTitle;
	}
	
}
