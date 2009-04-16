package com.netifera.platform.net.daemon.sniffing.extend;

import com.netifera.platform.api.dispatcher.DispatchException;
import com.netifera.platform.api.dispatcher.DispatchMismatchException;
import com.netifera.platform.api.dispatcher.IMessageDispatcher;
import com.netifera.platform.api.dispatcher.IMessageHandler;
import com.netifera.platform.api.dispatcher.IMessenger;
import com.netifera.platform.api.dispatcher.IProbeMessage;
import com.netifera.platform.api.dispatcher.MessengerException;
import com.netifera.platform.api.log.ILogger;
import com.netifera.platform.net.internal.daemon.probe.CancelCaptureFile;
import com.netifera.platform.net.internal.daemon.probe.CaptureFileValid;
import com.netifera.platform.net.internal.daemon.probe.RequestInterfaceInformation;
import com.netifera.platform.net.internal.daemon.probe.RequestModuleInformation;
import com.netifera.platform.net.internal.daemon.probe.RunCaptureFile;
import com.netifera.platform.net.internal.daemon.probe.SetInterfaceEnableState;
import com.netifera.platform.net.internal.daemon.probe.SetModuleEnableState;
import com.netifera.platform.net.internal.daemon.probe.SniffingDaemonStatus;
import com.netifera.platform.net.internal.daemon.probe.StartSniffingDaemon;
import com.netifera.platform.net.internal.daemon.probe.StopSniffingDaemon;

public class SniffingDaemonMessageDispatcher {
	
	private final ISniffingDaemonMessageHandler handler;
	private ILogger logger;
	
	public SniffingDaemonMessageDispatcher(ISniffingDaemonMessageHandler handler) {
		this.handler = handler;
	}

	void setLogger(ILogger logger) {
		this.logger = logger;
	}
	
	void registerHandlers(IMessageDispatcher dispatcher) {
		IMessageHandler msgHandler = new IMessageHandler() {

			public void call(IMessenger messenger, IProbeMessage message)
					throws DispatchException {
				try {
					dispatch(messenger, message);
				} catch (MessengerException e) {
					if(logger != null)
						logger.warning("Error sending message response " + e.getMessage());
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

}
