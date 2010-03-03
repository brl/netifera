package com.netifera.platform.system.privd;

public interface IPrivilegeDaemon {
	IPrivilegeDaemonLaunchStatus getDaemonLaunchStatus();
	boolean isDaemonAvailable();
	int openBPF();
	int openSocket(int family, int type, int protocol);
	String getLastError();
	boolean authenticate(String password);
}
