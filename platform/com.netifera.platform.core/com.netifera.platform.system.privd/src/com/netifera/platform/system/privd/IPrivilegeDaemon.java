package com.netifera.platform.system.privd;

public interface IPrivilegeDaemon {
	enum PrivilegeDaemonStatus {
		STATUS_UNCONNECTED,
		STATUS_OK,
		STATUS_NOTFOUND,
		STATUS_NOTSETUID,
		STATUS_CONFIG_MISSING
	}
	
	PrivilegeDaemonStatus getDaemonStatus();
	boolean isDaemonAvailable();
	int openBPF();
	int openSocket(int family, int type, int protocol);
	String getLastError();

}
