package com.netifera.platform.host.filesystem.probe;

import java.io.IOException;

import com.netifera.platform.api.dispatcher.DispatchException;
import com.netifera.platform.api.dispatcher.DispatchMismatchException;
import com.netifera.platform.api.dispatcher.IMessageDispatcher;
import com.netifera.platform.api.dispatcher.IMessageDispatcherService;
import com.netifera.platform.api.dispatcher.IMessageHandler;
import com.netifera.platform.api.dispatcher.IMessenger;
import com.netifera.platform.api.dispatcher.IProbeMessage;
import com.netifera.platform.api.dispatcher.MessengerException;
import com.netifera.platform.api.log.ILogManager;
import com.netifera.platform.api.log.ILogger;
import com.netifera.platform.host.filesystem.File;
import com.netifera.platform.host.filesystem.IFileSystem;
import com.netifera.platform.host.filesystem.LocalFileSystem;

public class FileSystem {
	private ILogger logger;
	private IFileSystem fileSystem;
	private final String rootPrefix;
	
	public FileSystem() {
		fileSystem = new LocalFileSystem();
		if(System.getProperty("com.netifera.filesystemprefix") != null) {
			rootPrefix = System.getProperty("com.netifera.filesystemprefix");
		} else {
			rootPrefix = "";
		}
	}
	
	private void getDirectoryListing(IMessenger messenger, GetDirectoryListing message) {
		try {
			File[] files = fileSystem.getDirectoryList(rootPrefix + message.getDirectoryPath());
			for(File f : files) {
				if(rootPrefix.length()>0 && f.getAbsolutePath().startsWith(rootPrefix)) {
					String realPath = f.getAbsolutePath().substring(rootPrefix.length());
					f.setPath(realPath);
				}
			}
			messenger.emitMessage(message.createResponse(files));
		} catch(IOException e) {
			logger.warning("Error sending message response: " + e.getMessage());
			e.printStackTrace();
		} catch (MessengerException e) {
			logger.warning("Error sending message response: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	private void getRoots(IMessenger messenger, GetRoots message) {
		try {
			File[] files = fileSystem.getRoots();
			messenger.emitMessage(message.createResponse(files));
		} catch(MessengerException e) {
			logger.warning("Error sending message response: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	private void registerHandlers(IMessageDispatcher dispatcher) {
		IMessageHandler handler = new IMessageHandler() {

			public void call(IMessenger messenger, IProbeMessage message)
					throws DispatchException {
				try {
					dispatch(messenger, message);
				} catch(MessengerException e) {
					logger.warning("Error sending message response: " + e.getMessage());
				}
			}
		};
		
		dispatcher.registerMessageHandler(GetDirectoryListing.ID, handler);
		dispatcher.registerMessageHandler(GetRoots.ID, handler);
	}
	
	private void dispatch(IMessenger messenger, IProbeMessage message) throws DispatchMismatchException, MessengerException {
		if(message instanceof GetDirectoryListing) {
			getDirectoryListing(messenger, (GetDirectoryListing) message);
		} else if(message instanceof GetRoots) {
			getRoots(messenger, (GetRoots) message);
		}
	}
	
	protected void setLogManager(ILogManager logManager) {
		logger = logManager.getLogger("File System");
	}
	
	protected void unsetLogManager(ILogManager logManager) {
	}
	
	protected void setDispatcher(IMessageDispatcherService dispatcher) {
		registerHandlers(dispatcher.getServerDispatcher());
	}
	
	protected void unsetDispatcher(IMessageDispatcherService dispatcher) {
	}
}
