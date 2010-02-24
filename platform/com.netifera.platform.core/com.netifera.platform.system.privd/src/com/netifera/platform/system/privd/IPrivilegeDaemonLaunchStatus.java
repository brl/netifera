package com.netifera.platform.system.privd;

public interface IPrivilegeDaemonLaunchStatus {
	boolean isConnected();
	boolean launchFailed();
	String getLaunchFailureMessage();
}
