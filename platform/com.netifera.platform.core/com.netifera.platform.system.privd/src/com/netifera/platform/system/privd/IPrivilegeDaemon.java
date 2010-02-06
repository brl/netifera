package com.netifera.platform.system.privd;

public interface IPrivilegeDaemon {
	boolean isDaemonAvailable();
	int openBPF();
	int openSocket(int family, int type, int protocol);
	String getLastError();

}
