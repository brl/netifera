package com.netifera.platform.internal.system.privd;

import java.util.Arrays;
import java.util.List;

import com.netifera.platform.api.log.ILogger;

public class PrivilegeDaemonConnection {
	private final static String PRIVD_EXECUTABLE = "netifera_privd";
	private final List<String> searchPaths = Arrays.asList("/usr/local/bin");
	private final ILogger logger;
	private final PrivilegeDaemonNative jni;
	
	PrivilegeDaemonConnection(ILogger logger, PrivilegeDaemonNative jni) {
		this.logger = logger;
		this.jni = jni;
	}
}
