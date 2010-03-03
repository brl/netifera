package com.netifera.platform.internal.system.privd;

import com.netifera.platform.system.privd.IPrivilegeDaemonLaunchStatus;

public class PrivilegeDaemonLaunchStatus implements IPrivilegeDaemonLaunchStatus {

	static PrivilegeDaemonLaunchStatus createUnconnectedStatus() {
		return new PrivilegeDaemonLaunchStatus(StatusType.UNCONNECTED, "Not Connected.");
	}

	static PrivilegeDaemonLaunchStatus createWaitingAuthenticationStatus() {
		return new PrivilegeDaemonLaunchStatus(StatusType.WAITING_AUTHENTICATION, "Need authentication");
	}
	
	static PrivilegeDaemonLaunchStatus createConnectedStatus() {
		return new PrivilegeDaemonLaunchStatus(StatusType.CONNECTED, "No Error.");
	}
	
	static PrivilegeDaemonLaunchStatus createLaunchFailed(String failureMessage) {
		return new PrivilegeDaemonLaunchStatus(StatusType.LAUNCH_FAILED, failureMessage);
	}
	
	private final StatusType statusType;
	private final String launchFailureMessage;
	

	private PrivilegeDaemonLaunchStatus(StatusType type, String message) {
		this.statusType = type;
		this.launchFailureMessage = message;
	}
	
	public String getLaunchFailureMessage() {
		return launchFailureMessage;
	}

	public StatusType getStatusType() {
		return statusType;
	}

}
