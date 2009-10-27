package com.netifera.platform.host.processes;


public interface IProcessService {
	Process[] getProcessList();
	boolean kill(int pid);
}
