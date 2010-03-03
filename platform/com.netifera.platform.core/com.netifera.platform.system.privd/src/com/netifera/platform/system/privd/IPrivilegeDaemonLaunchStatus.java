package com.netifera.platform.system.privd;

public interface IPrivilegeDaemonLaunchStatus {
	enum StatusType {
		UNCONNECTED,
		WAITING_AUTHENTICATION,
		CONNECTED,
		LAUNCH_FAILED
	};
	StatusType getStatusType();
	String getLaunchFailureMessage();
}
