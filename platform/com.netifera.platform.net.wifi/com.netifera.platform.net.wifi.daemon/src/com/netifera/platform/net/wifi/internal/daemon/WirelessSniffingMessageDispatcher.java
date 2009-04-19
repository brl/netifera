package com.netifera.platform.net.wifi.internal.daemon;

import com.netifera.platform.api.dispatcher.DispatchException;
import com.netifera.platform.api.dispatcher.DispatchMismatchException;
import com.netifera.platform.api.dispatcher.IMessageDispatcher;
import com.netifera.platform.api.dispatcher.IMessageHandler;
import com.netifera.platform.api.dispatcher.IMessenger;
import com.netifera.platform.api.dispatcher.IProbeMessage;
import com.netifera.platform.api.dispatcher.MessengerException;
import com.netifera.platform.net.wifi.internal.daemon.remote.RequestWirelessModuleInformation;
import com.netifera.platform.net.wifi.internal.daemon.remote.SetWirelessModuleEnableState;

public class WirelessSniffingMessageDispatcher {
	private final IMessageHandler messageHandler;
	private final WirelessSniffingDaemon daemon;
	
	public WirelessSniffingMessageDispatcher(WirelessSniffingDaemon daemon) {
		this.messageHandler = createMessageHandler();
		this.daemon = daemon;
	}
	
	private IMessageHandler createMessageHandler() {
		return new IMessageHandler() {

			public void call(IMessenger messenger, IProbeMessage message)
					throws DispatchException {
				try {
					dispatch(messenger, message);
				} catch(MessengerException e) {
					daemon.getLogger().warning("Error sending message response " + e.getMessage());
				} catch(Exception e) {
					reportUnexpectedException(messenger, message, e);
				}
			}
			
		};
	}
	
	public void registerHandlers(IMessageDispatcher dispatcher) {
		dispatcher.registerMessageHandler(RequestWirelessModuleInformation.ID, messageHandler);
		dispatcher.registerMessageHandler(SetWirelessModuleEnableState.ID, messageHandler);
	}
	
	private void dispatch(IMessenger messenger, IProbeMessage message) throws DispatchMismatchException, MessengerException {
		if(message instanceof RequestWirelessModuleInformation) {
			daemon.requestWirelessModuleInformation(messenger, (RequestWirelessModuleInformation) message);
		} else if(message instanceof SetWirelessModuleEnableState) {
			daemon.setWirelessModuleEnableState(messenger, (SetWirelessModuleEnableState) message);
		} else {
			throw new DispatchMismatchException(message);
		}
	}
	
	private void reportUnexpectedException(IMessenger messenger, IProbeMessage message, Exception ex) {
		try {
			daemon.getLogger().warning("Unexpected exception processing [" + message + "] : " + ex);
			messenger.respondError(message, "Unexpected Exception processing [" + message + "] : " + ex);
		} catch(MessengerException e) {
			daemon.getLogger().error("Failed to send unexpected exception error", e);
		}
	}
}
