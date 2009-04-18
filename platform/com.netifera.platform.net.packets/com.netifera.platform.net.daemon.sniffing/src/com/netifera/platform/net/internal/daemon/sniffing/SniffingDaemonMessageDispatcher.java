package com.netifera.platform.net.internal.daemon.sniffing;

import com.netifera.platform.api.dispatcher.DispatchException;
import com.netifera.platform.api.dispatcher.DispatchMismatchException;
import com.netifera.platform.api.dispatcher.IMessageDispatcher;
import com.netifera.platform.api.dispatcher.IMessageHandler;
import com.netifera.platform.api.dispatcher.IMessenger;
import com.netifera.platform.api.dispatcher.IProbeMessage;
import com.netifera.platform.api.dispatcher.MessengerException;
import com.netifera.platform.api.log.ILogger;
import com.netifera.platform.net.daemon.sniffing.extend.ISniffingDaemonMessageHandler;
import com.netifera.platform.net.internal.daemon.remote.CancelCaptureFile;
import com.netifera.platform.net.internal.daemon.remote.CaptureFileValid;
import com.netifera.platform.net.internal.daemon.remote.RequestInterfaceInformation;
import com.netifera.platform.net.internal.daemon.remote.RequestModuleInformation;
import com.netifera.platform.net.internal.daemon.remote.RunCaptureFile;
import com.netifera.platform.net.internal.daemon.remote.SetInterfaceEnableState;
import com.netifera.platform.net.internal.daemon.remote.SetModuleEnableState;
import com.netifera.platform.net.internal.daemon.remote.SniffingDaemonStatus;
import com.netifera.platform.net.internal.daemon.remote.StartSniffingDaemon;
import com.netifera.platform.net.internal.daemon.remote.StopSniffingDaemon;

public class SniffingDaemonMessageDispatcher {
	
	private final ISniffingDaemonMessageHandler handler;
	private ILogger logger;
	private IMessageDispatcher dispatcher;
	private boolean isInitialized;
	
	public SniffingDaemonMessageDispatcher(ISniffingDaemonMessageHandler daemon) {
		this.handler = daemon;
	}

	public void setServices(ILogger logger, IMessageDispatcher dispatcher) {
		this.logger = logger;
		this.dispatcher = dispatcher;
		this.isInitialized = true;
	}
	private void verifyInitialized() {
		if(!isInitialized) {
			throw new IllegalStateException("Sniffing Daemon Message Dispatcher subsystem is not initialized");
		}
	}
	
	public void registerHandlers() {
		verifyInitialized();
		IMessageHandler msgHandler = new IMessageHandler() {

			public void call(IMessenger messenger, IProbeMessage message)
					throws DispatchException {
				try {
					dispatch(messenger, message);
				} catch (MessengerException e) {
					logger.warning("Error sending message response " + e.getMessage());
				} catch(Exception e) {
					reportUnexpectedException(messenger, message, e);
				}
			}
		};
		
		dispatcher.registerMessageHandler(RequestInterfaceInformation.ID, msgHandler);
		dispatcher.registerMessageHandler(RequestModuleInformation.ID, msgHandler);
		dispatcher.registerMessageHandler(SetInterfaceEnableState.ID, msgHandler);
		dispatcher.registerMessageHandler(SetModuleEnableState.ID, msgHandler);
		dispatcher.registerMessageHandler(StartSniffingDaemon.ID, msgHandler);
		dispatcher.registerMessageHandler(StopSniffingDaemon.ID, msgHandler);
		dispatcher.registerMessageHandler(CaptureFileValid.ID, msgHandler);
		dispatcher.registerMessageHandler(SniffingDaemonStatus.ID, msgHandler);
		dispatcher.registerMessageHandler(RunCaptureFile.ID, msgHandler);
		dispatcher.registerMessageHandler(CancelCaptureFile.ID, msgHandler);
	}
	
	private void dispatch(IMessenger messenger, IProbeMessage message) throws DispatchMismatchException, MessengerException {
		if(message instanceof RequestInterfaceInformation) {
			handler.requestInterfaceInformation(messenger, (RequestInterfaceInformation) message);
		} else if(message instanceof RequestModuleInformation) {
			handler.requestModuleInformation(messenger, (RequestModuleInformation) message);
		} else if(message instanceof SetInterfaceEnableState) {
			handler.setInterfaceEnableState(messenger, (SetInterfaceEnableState) message);
		} else if(message instanceof SetModuleEnableState) {
			handler.setModuleEnableState(messenger, (SetModuleEnableState) message);
		} else if(message instanceof StartSniffingDaemon) {
			handler.startSniffingDaemon(messenger, (StartSniffingDaemon) message);
		} else if(message instanceof StopSniffingDaemon) {
			handler.stopSniffingDaemon(messenger, (StopSniffingDaemon) message);
		} else if(message instanceof CaptureFileValid) {
			handler.captureFileValid(messenger, (CaptureFileValid) message);
		} else if(message instanceof SniffingDaemonStatus) {
			handler.sniffingDaemonStatus(messenger, (SniffingDaemonStatus) message);
		} else if(message instanceof RunCaptureFile) {
			handler.runCaptureFile(messenger, (RunCaptureFile) message);
		} else if(message instanceof CancelCaptureFile) {
			handler.cancelCaptureFile(messenger, (CancelCaptureFile) message);
		}else {
			throw new DispatchMismatchException(message);
		}
	}

	private void reportUnexpectedException(IMessenger messenger, IProbeMessage message, Exception ex) {
		try {
			messenger.respondError(message, "Unexpected Exception processing [" + message + "] : " + ex);
		} catch (MessengerException e) {
			logger.error("Failed to send unexpected exception error", e);
		}
	}
}
