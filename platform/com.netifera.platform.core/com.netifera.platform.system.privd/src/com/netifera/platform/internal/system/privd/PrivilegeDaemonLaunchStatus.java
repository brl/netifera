package com.netifera.platform.internal.system.privd;

import com.netifera.platform.system.privd.IPrivilegeDaemonLaunchStatus;

public class PrivilegeDaemonLaunchStatus implements IPrivilegeDaemonLaunchStatus {

	static PrivilegeDaemonLaunchStatus createUnconnectedStatus() {
		return new PrivilegeDaemonLaunchStatus(false, false, "Not Connected.");
	}
	
	static PrivilegeDaemonLaunchStatus createConnectedStatus() {
		return new PrivilegeDaemonLaunchStatus(true, false, "No Error.");
	}
	
	static PrivilegeDaemonLaunchStatus createLaunchFailed(String failureMessage) {
		return new PrivilegeDaemonLaunchStatus(false, true, failureMessage);
	}
	
	private final String launchFailureMessage;
	private final boolean connected;
	private final boolean failed;
	
	private PrivilegeDaemonLaunchStatus(boolean connected, boolean failed, String message) {
		this.connected = connected;
		this.failed = failed;
		this.launchFailureMessage = message;
	}
	
	public String getLaunchFailureMessage() {
		return launchFailureMessage;
	}

	public boolean isConnected() {
		return connected;
	}

	public boolean launchFailed() {
		return failed;
	}

}
