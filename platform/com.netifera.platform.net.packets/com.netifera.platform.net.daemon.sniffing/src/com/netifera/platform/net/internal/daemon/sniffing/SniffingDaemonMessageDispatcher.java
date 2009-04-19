package com.netifera.platform.net.internal.daemon.sniffing;

import com.netifera.platform.api.dispatcher.DispatchException;
import com.netifera.platform.api.dispatcher.DispatchMismatchException;
import com.netifera.platform.api.dispatcher.IMessageDispatcher;
import com.netifera.platform.api.dispatcher.IMessageHandler;
import com.netifera.platform.api.dispatcher.IMessenger;
import com.netifera.platform.api.dispatcher.IProbeMessage;
import com.netifera.platform.api.dispatcher.MessengerException;
import com.netifera.platform.net.daemon.sniffing.extend.AbstractSniffingDaemon;
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
	
	private final AbstractSniffingDaemon daemon;
	private final IMessageHandler messageHandler;
	private IMessageDispatcher dispatcher;
	
	public SniffingDaemonMessageDispatcher(AbstractSniffingDaemon daemon) {
		this.daemon = daemon;
		this.messageHandler = createMessageHandler();
	}

	private IMessageHandler createMessageHandler() {
		return new IMessageHandler() {

			public void call(IMessenger messenger, IProbeMessage message)
					throws DispatchException {
				try {
					dispatch(messenger, message);
				} catch (MessengerException e) {
					daemon.getLogger().warning("Error sending message response " + e.getMessage());
				} catch(Exception e) {
					reportUnexpectedException(messenger, message, e);
				}
			}
		};
		
	}
	public void registerHandlers(IMessageDispatcher dispatcher) {
		this.dispatcher = dispatcher;
		register(RequestInterfaceInformation.ID);
		register(RequestModuleInformation.ID);
		register(SetInterfaceEnableState.ID);
		register(SetModuleEnableState.ID);
		register(StartSniffingDaemon.ID);
		register(StopSniffingDaemon.ID);
		register(CancelCaptureFile.ID);
		register(CaptureFileValid.ID);
		register(SniffingDaemonStatus.ID);
		register(RunCaptureFile.ID);
	}
	
	private void register(String id) {
		dispatcher.registerMessageHandler(daemon.getMessagePrefix() + id, messageHandler);
	}
	private void dispatch(IMessenger messenger, IProbeMessage message) throws DispatchMismatchException, MessengerException {
		if(message instanceof RequestInterfaceInformation) {
			daemon.requestInterfaceInformation(messenger, (RequestInterfaceInformation) message);
		} else if(message instanceof RequestModuleInformation) {
			daemon.requestModuleInformation(messenger, (RequestModuleInformation) message);
		} else if(message instanceof SetInterfaceEnableState) {
			daemon.setInterfaceEnableState(messenger, (SetInterfaceEnableState) message);
		} else if(message instanceof SetModuleEnableState) {
			daemon.setModuleEnableState(messenger, (SetModuleEnableState) message);
		} else if(message instanceof StartSniffingDaemon) {
			daemon.startSniffingDaemon(messenger, (StartSniffingDaemon) message);
		} else if(message instanceof StopSniffingDaemon) {
			daemon.stopSniffingDaemon(messenger, (StopSniffingDaemon) message);
		} else if(message instanceof CaptureFileValid) {
			daemon.captureFileValid(messenger, (CaptureFileValid) message);
		} else if(message instanceof SniffingDaemonStatus) {
			daemon.sniffingDaemonStatus(messenger, (SniffingDaemonStatus) message);
		} else if(message instanceof RunCaptureFile) {
			daemon.runCaptureFile(messenger, (RunCaptureFile) message);
		} else if(message instanceof CancelCaptureFile) {
			daemon.cancelCaptureFile(messenger, (CancelCaptureFile) message);
		}else {
			throw new DispatchMismatchException(message);
		}
	}

	private void reportUnexpectedException(IMessenger messenger, IProbeMessage message, Exception ex) {
		try {
			messenger.respondError(message, "Unexpected Exception processing [" + message + "] : " + ex);
		} catch (MessengerException e) {
			daemon.getLogger().error("Failed to send unexpected exception error", e);
		}
	}
}
