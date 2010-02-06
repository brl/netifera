package com.netifera.platform.internal.system.privd;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.netifera.platform.api.log.ILogManager;
import com.netifera.platform.api.log.ILogger;
import com.netifera.platform.internal.system.privd.messages.IMessageResponse;
import com.netifera.platform.internal.system.privd.messages.MessageException;
import com.netifera.platform.internal.system.privd.messages.MessageResponseError;
import com.netifera.platform.internal.system.privd.messages.MessageResponseFd;
import com.netifera.platform.internal.system.privd.messages.MessageResponseStartup;
import com.netifera.platform.internal.system.privd.messages.MessageSender;
import com.netifera.platform.internal.system.privd.messages.ResponseType;
import com.netifera.platform.system.privd.IPrivilegeDaemon;

public class PrivilegeDaemon implements IPrivilegeDaemon {
	private final static String PRIVD_EXECUTABLE = "netifera_privd";	
	private ILogger logger;
	private final List<String> searchPaths = new ArrayList<String>();
	private String lastErrorMessage;
	private final PrivilegeDaemonNative jni;
	private final MessageSender sender;
	
	public PrivilegeDaemon() {
		searchPaths.add("/usr/local/bin");
		jni = new PrivilegeDaemonNative();
		sender = new MessageSender(jni);
	}

	public synchronized boolean isDaemonAvailable() {
		if(jni.isDaemonRunning())
			return true;
		
		final String daemonPath = findDaemonExecutable();
		if(daemonPath == null) {
			setErrorMessage("Could not locate privilege daemon executable");
			return false;
		}
		
		System.out.println("Daemon path is " + daemonPath);
		return startDaemon(daemonPath);
			
	}
	
	private boolean startDaemon(String daemonPath) {
		if(jni.startDaemon(daemonPath) == -1) {
			setErrorMessage("Could not start privilege daemon : " + jni.getLastErrorMessage());
			return false;
		}
		try {
			return receiveStartupMessage();
		} catch (MessageException e) {
			setErrorMessage("Error receiving startup message : " + e.getMessage());
			return false;
		}
	}
	
	private boolean receiveStartupMessage() throws MessageException {
		System.out.println("receivestartupmessage");
		final IMessageResponse response = sender.receiveResponse();
		if(response.getType() != ResponseType.PRIVD_RESPONSE_STARTUP) {
			setErrorMessage("Unexpected startup message type received");
			return false;
		}
		MessageResponseStartup startupMessage = (MessageResponseStartup) response;
		
		System.out.println("startup " + startupMessage);
		return false;
		
	}
	
	private void setErrorMessage(String message) {
		logger.error(message);
		lastErrorMessage = message;
	}
	
	private String findDaemonExecutable() {
		for(String path : searchPaths) {
			if(verifyDaemonPath(path))
				return pathToExecutablePath(path);
		}
		return null;
	}
	private boolean verifyDaemonPath(String path) {
		if(path == null)
			return false;
		
		logger.info("Searching for privilege daemon at : " + pathToExecutablePath(path));
		
		final File file = new File(path, PRIVD_EXECUTABLE);
		return file.exists();
	}
	
	private String pathToExecutablePath(String basePath) {
		if(basePath.endsWith(File.separator)) {
			return basePath + PRIVD_EXECUTABLE;
		}
		return basePath + File.separator + PRIVD_EXECUTABLE;
	}
	
	protected void setLogManager(ILogManager manager) {
		this.logger = manager.getLogger("Privilege Daemon");
	}
	
	protected void unsetLogManager(ILogManager manager) {
		
	}

	public String getLastError() {
		return lastErrorMessage;
	}

	public int openBPF() {
		final IMessageResponse response = sender.sendOpenBPF();
		return processResponseFd(response);
	}
	
	public int openSocket(int family, int type, int protocol) {
		final IMessageResponse response = sender.sendOpenSocket(family, type, protocol);
		return processResponseFd(response);
	}
	
	private int processResponseFd(IMessageResponse response) {
		switch(response.getType()) {
		case PRIVD_RESPONSE_FD:
			final int fd = ((MessageResponseFd)response).getReceivedFd();
			if(fd == -1) 
				setErrorMessage("Did not receive file descriptor as expected");
			return fd;
			
		case PRIVD_RESPONSE_ERROR:
			setErrorMessage("Error: " + ((MessageResponseError)response).getErrorString());
			return -1;
		default:
			setErrorMessage("Unexpected response type " + response.getType());
			return -1;
		}
	}

}
