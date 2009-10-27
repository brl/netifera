package com.netifera.platform.host.processes.probe;

import org.osgi.service.component.ComponentContext;

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
import com.netifera.platform.host.processes.IProcessService;
import com.netifera.platform.host.processes.linux.LinuxProcessService;

public class ProcessServiceBridge {
	
	private ILogger logger;
	IProcessService service;
	
	protected void activate(ComponentContext ctx) {
		service = new LinuxProcessService(logger); //TODO
	}
	
	protected void deactivate(ComponentContext ctx) {
		
	}
	
	private void getProcessList(IMessenger messenger, GetProcessList message) {
		GetProcessList response = message.createResponse(service.getProcessList());
		try {
			messenger.emitMessage(response);
		} catch (MessengerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		};
		
	}
	private void registerHandlers(IMessageDispatcher dispatcher) {
		IMessageHandler handler = new IMessageHandler() {

			public void call(IMessenger messenger, IProbeMessage message)
					throws DispatchException {
				try {
					dispatch(messenger, message);
				} catch(MessengerException e) {
					logger.warning("Error sending message response " + e.getMessage());
				}
				
			}
			
		};
		
		dispatcher.registerMessageHandler(GetProcessList.ID, handler);
		
	}
	
	
	private void dispatch(IMessenger messenger, IProbeMessage message) throws DispatchMismatchException, MessengerException {
		if(message instanceof GetProcessList) {
			getProcessList(messenger, (GetProcessList)message);
		}
	}
	
	
	protected void setDispatcher(IMessageDispatcherService dispatcher) {
		registerHandlers(dispatcher.getServerDispatcher());
	}
	
	protected void unsetDispatcher(IMessageDispatcherService dispatcher) {
		
	}
	protected void setLogManager(ILogManager logManager) {
		this.logger = logManager.getLogger("Process Service");
	}
	
	protected void unsetLogManager(ILogManager logManager) {
		
	}
}
