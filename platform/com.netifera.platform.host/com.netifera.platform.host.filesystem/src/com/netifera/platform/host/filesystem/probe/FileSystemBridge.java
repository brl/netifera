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
import com.netifera.platform.services.IServiceFactory;

public class FileSystemBridge {
	private ILogger logger;
	private IServiceFactory serviceFactory;
	
//	private Map<String,IFileSystem> fileSystems = new HashMap<String,IFileSystem>();
	
	private void getRoots(IMessenger messenger, GetRoots message) {
		try {
			IFileSystem fileSystem = (IFileSystem) serviceFactory.create(IFileSystem.class, message.getFileSystemURL());
			File[] files = fileSystem.getRoots();
			messenger.emitMessage(message.createResponse(files));
		} catch(MessengerException e) {
			logger.warning("Error sending message response: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private void getDirectoryListing(IMessenger messenger, GetDirectoryListing message) {
		try {
			IFileSystem fileSystem = (IFileSystem) serviceFactory.create(IFileSystem.class, message.getFileSystemURL());
			File[] files = fileSystem.getDirectoryList(message.getPath());
			messenger.emitMessage(message.createResponse(files));
		} catch(IOException e) {
			logger.warning("Error sending message response: " + e.getMessage());
			e.printStackTrace();
		} catch (MessengerException e) {
			logger.warning("Error sending message response: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private void delete(IMessenger messenger, DeleteFile message) {
		try {
			IFileSystem fileSystem = (IFileSystem) serviceFactory.create(IFileSystem.class, message.getFileSystemURL());
			boolean result = fileSystem.delete(message.getPath());
			messenger.emitMessage(message.createResponse(result));
		} catch(IOException e) {
			logger.warning("Error sending message response: " + e.getMessage());
			e.printStackTrace();
		} catch (MessengerException e) {
			logger.warning("Error sending message response: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private void deleteDirectory(IMessenger messenger, DeleteDirectory message) {
		try {
			IFileSystem fileSystem = (IFileSystem) serviceFactory.create(IFileSystem.class, message.getFileSystemURL());
			boolean result = fileSystem.deleteDirectory(message.getPath());
			messenger.emitMessage(message.createResponse(result));
		} catch(IOException e) {
			logger.warning("Error sending message response: " + e.getMessage());
			e.printStackTrace();
		} catch (MessengerException e) {
			logger.warning("Error sending message response: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private void createDirectory(IMessenger messenger, CreateDirectory message) {
		try {
			IFileSystem fileSystem = (IFileSystem) serviceFactory.create(IFileSystem.class, message.getFileSystemURL());
			File newDirectory = fileSystem.createDirectory(message.getPath());
			messenger.emitMessage(message.createResponse(newDirectory));
		} catch(IOException e) {
			logger.warning("Error sending message response: " + e.getMessage());
			e.printStackTrace();
		} catch (MessengerException e) {
			logger.warning("Error sending message response: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private void rename(IMessenger messenger, Rename message) {
		try {
			IFileSystem fileSystem = (IFileSystem) serviceFactory.create(IFileSystem.class, message.getFileSystemURL());
			boolean result = fileSystem.rename(message.getOldName(), message.getNewName());
			messenger.emitMessage(message.createResponse(result));
		} catch(IOException e) {
			logger.warning("Error sending message response: " + e.getMessage());
			e.printStackTrace();
		} catch (MessengerException e) {
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
		
		dispatcher.registerMessageHandler(GetRoots.ID, handler);
		dispatcher.registerMessageHandler(GetDirectoryListing.ID, handler);
		dispatcher.registerMessageHandler(DeleteFile.ID, handler);
		dispatcher.registerMessageHandler(DeleteDirectory.ID, handler);
		dispatcher.registerMessageHandler(CreateDirectory.ID, handler);
		dispatcher.registerMessageHandler(Rename.ID, handler);
	}
	
	private void dispatch(IMessenger messenger, IProbeMessage message) throws DispatchMismatchException, MessengerException {
		if (message instanceof GetRoots) {
			getRoots(messenger, (GetRoots) message);
		} else if (message instanceof GetDirectoryListing) {
			getDirectoryListing(messenger, (GetDirectoryListing) message);
		} else if (message instanceof DeleteFile) {
			delete(messenger, (DeleteFile) message);
		} else if (message instanceof DeleteDirectory) {
			deleteDirectory(messenger, (DeleteDirectory) message);
		} else if (message instanceof CreateDirectory) {
			createDirectory(messenger, (CreateDirectory) message);
		} else if (message instanceof Rename) {
			rename(messenger, (Rename) message);
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
	
	protected void setServiceFactory(IServiceFactory factory) {
		this.serviceFactory = factory;
	}
	
	protected void unsetServiceFactory(IServiceFactory factory) {
	}
}
