package com.netifera.platform.internal.system.privd;

import java.io.File;
import java.util.Arrays;
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
import com.netifera.platform.internal.system.privd.messages.StartupType;
import com.netifera.platform.system.privd.IPrivilegeDaemon;
import com.netifera.platform.system.privd.IPrivilegeDaemonLaunchStatus;

public class PrivilegeDaemon implements IPrivilegeDaemon {
	private ILogger logger;
	private final static String PRIVD_EXECUTABLE = "netifera_privd";
	private final List<String> searchPaths = Arrays.asList("/usr/local/bin");
	private final MessageSender sender = new MessageSender();
	
	private String lastErrorMessage = "No error.";
	private IPrivilegeDaemonLaunchStatus launchStatus = PrivilegeDaemonLaunchStatus.createUnconnectedStatus();
	private String authenticationPassword;

	public IPrivilegeDaemonLaunchStatus getDaemonLaunchStatus() {
		return launchStatus;
	}
	
	public boolean isDaemonAvailable() {
		return startDaemonIfNeeded();
	}
	
	protected void setLogManager(ILogManager manager) {
		this.logger = manager.getLogger("Privilege Daemon");
	}
	
	protected void unsetLogManager(ILogManager manager) {
		
	}

	public String getLastError() {
		return lastErrorMessage;
	}
	
	public boolean authenticate(String password) {
		switch(launchStatus.getStatusType()) {
		case CONNECTED:
			return true;
		case UNCONNECTED:
			return startAndAuthenticate(password);
		case LAUNCH_FAILED:
			return false;
		case WAITING_AUTHENTICATION:
			authenticationPassword = password;
			return doAuthentication();
		}
		return false;
	}

	private boolean startAndAuthenticate(String password) {
		findPathAndStartDaemon();
		switch(launchStatus.getStatusType()) {
		case CONNECTED:
			return true;
		case LAUNCH_FAILED:
		case UNCONNECTED:
			return false;
		case WAITING_AUTHENTICATION:
			authenticationPassword = password;
			return doAuthentication();
		}
		return false;
	}

	public int openBPF() {
		logger.debug("openBPF called.");
		if(!startDaemonIfNeeded()) {
			setErrorMessage("Privilege daemon could not be launched");
			return -1; 
		}
		try {
			IMessageResponse response = sender.sendOpenBPF();
			return processResponseFd(response);
		} catch (MessageException e) {
			setErrorMessage("openBFP request to privd failed : "+ e.getMessage());
			return -1;
		}
	}
	
	public int openSocket(int family, int type, int protocol) {
		if(!startDaemonIfNeeded()) {
			setErrorMessage("Privilege daemon could not be launched");
			return -1; 
		}
		try {
			IMessageResponse response = sender.sendOpenSocket(family, type, protocol);
			return processResponseFd(response);
		} catch (MessageException e) {
			setErrorMessage("openSocket request to privd failed : "+ e.getMessage());
			return -1;
		}
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
	
	private synchronized boolean startDaemonIfNeeded() {
		switch(launchStatus.getStatusType()) {
		case CONNECTED:
			return true;
		case LAUNCH_FAILED:
		case WAITING_AUTHENTICATION:
			return false;
		case UNCONNECTED:
			return findPathAndStartDaemon();
		}
		return false;
	}
	
	private boolean findPathAndStartDaemon() {
		final String daemonPath = findDaemonExecutable();
		if(daemonPath == null) {
			launchStatus = PrivilegeDaemonLaunchStatus.createLaunchFailed("Could not locate privilege daemon executable.");
			return false;
		}
		
		if(!startDaemon(daemonPath)) 
			return false;
		launchStatus = PrivilegeDaemonLaunchStatus.createConnectedStatus();
		return true;
	}
	
	private boolean startDaemon(String daemonPath) {
		try {
			logger.info("Launching privilege daemon from path "+ daemonPath);
			sender.startDaemon(daemonPath);
		} catch (MessageException e) {
			final String msg = "Daemon launch failed : "+ e.getMessage();
			launchStatus = PrivilegeDaemonLaunchStatus.createLaunchFailed(msg);
			return false;
		}
		
		try {
			return receiveStartupMessage();
		} catch (MessageException e) {
			final String msg = "Error receiving startup message : "+ e.getMessage();
			launchStatus = PrivilegeDaemonLaunchStatus.createLaunchFailed(msg);
			return false;
		}	
	}
	
	private boolean receiveStartupMessage() throws MessageException {	
		MessageResponseStartup startupMessage = sender.readStartupMessage();
		StartupType type = startupMessage.getStartupType();
		if(type == StartupType.PRIVD_STARTUP_OK) 
			return true;
		
		final String errorMessage = startupMessage.getMessage();
		
		switch(type) {
		case PRIVD_STARTUP_NOT_ROOT:
		case PRIVD_STARTUP_INITIALIZATION_FAILED:
		case PRIVD_STARTUP_CONFIG_NOT_FOUND:
		case PRIVD_STARTUP_CONFIG_BAD_DATA:
		case PRIVD_STARTUP_CONFIG_BAD_PERMS:
			formatLaunchStatus(errorMessage);
			return false;
		case PRIVD_STARTUP_AUTHENTICATION_REQUIRED:
			return doAuthentication();
		default:
			return false;
		
		}
	}
	
	private void formatLaunchStatus(String message) {
		final String error = "Failed to launch privilege daemon" + ((message == null) ? ("") : (" : "+ message));
		launchStatus = PrivilegeDaemonLaunchStatus.createLaunchFailed(error);
	}
	
	private boolean doAuthentication() {
		if(authenticationPassword == null) {
			launchStatus = PrivilegeDaemonLaunchStatus.createWaitingAuthenticationStatus();
			return false;
		}

		try {
			final IMessageResponse response = sender.sendAuthenticate(authenticationPassword);
			if(response.getType() == ResponseType.PRIVD_RESPONSE_OK) {
				launchStatus = PrivilegeDaemonLaunchStatus.createConnectedStatus();
				return true;
			} else if (response.getType() == ResponseType.PRIVD_RESPONSE_AUTH_FAILED) {
				return false;
			} else if(response.getType() == ResponseType.PRIVD_RESPONSE_ERROR) {
				launchStatus = PrivilegeDaemonLaunchStatus.createLaunchFailed(((MessageResponseError)response).getErrorString());
				return false;
			} else {
				launchStatus = PrivilegeDaemonLaunchStatus.createLaunchFailed("Unexpected authentication response type : "+ response.getType());
				return false;
			}
		} catch (MessageException e) {
			launchStatus = PrivilegeDaemonLaunchStatus.createLaunchFailed("Error exchanging authentication messages : "+ e.getMessage());
			return false;
		}
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
}
