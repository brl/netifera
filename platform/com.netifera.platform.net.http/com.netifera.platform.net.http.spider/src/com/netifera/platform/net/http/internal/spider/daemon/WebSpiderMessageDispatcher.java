package com.netifera.platform.net.http.internal.spider.daemon;

import com.netifera.platform.api.dispatcher.DispatchException;
import com.netifera.platform.api.dispatcher.DispatchMismatchException;
import com.netifera.platform.api.dispatcher.IMessageDispatcher;
import com.netifera.platform.api.dispatcher.IMessageHandler;
import com.netifera.platform.api.dispatcher.IMessenger;
import com.netifera.platform.api.dispatcher.IProbeMessage;
import com.netifera.platform.api.dispatcher.MessengerException;
import com.netifera.platform.api.log.ILogger;
import com.netifera.platform.net.http.internal.spider.daemon.remote.GetSpiderConfiguration;
import com.netifera.platform.net.http.internal.spider.daemon.remote.GetSpiderStatus;
import com.netifera.platform.net.http.internal.spider.daemon.remote.SetSpiderConfiguration;
import com.netifera.platform.net.http.internal.spider.daemon.remote.StartSpider;
import com.netifera.platform.net.http.internal.spider.daemon.remote.StopSpider;

public class WebSpiderMessageDispatcher {
	
	private final IWebSpiderMessageHandler handler;
	private ILogger logger;
	private IMessageDispatcher dispatcher;
	private boolean isInitialized;
	
	public WebSpiderMessageDispatcher(IWebSpiderMessageHandler daemon) {
		this.handler = daemon;
	}

	public void setServices(ILogger logger, IMessageDispatcher dispatcher) {
		this.logger = logger;
		this.dispatcher = dispatcher;
		this.isInitialized = true;
	}
	private void verifyInitialized() {
		if(!isInitialized) {
			throw new IllegalStateException("Web Spider Message Dispatcher subsystem is not initialized");
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
		
		dispatcher.registerMessageHandler(GetSpiderConfiguration.ID, msgHandler);
		dispatcher.registerMessageHandler(SetSpiderConfiguration.ID, msgHandler);
		dispatcher.registerMessageHandler(StartSpider.ID, msgHandler);
		dispatcher.registerMessageHandler(StopSpider.ID, msgHandler);
		dispatcher.registerMessageHandler(GetSpiderStatus.ID, msgHandler);
	}
	
	private void dispatch(IMessenger messenger, IProbeMessage message) throws DispatchMismatchException, MessengerException {
		if(message instanceof GetSpiderConfiguration) {
			handler.getSpiderConfiguration(messenger, (GetSpiderConfiguration) message);
		} else if(message instanceof SetSpiderConfiguration) {
			handler.setSpiderConfiguration(messenger, (SetSpiderConfiguration) message);
		} else if(message instanceof StartSpider) {
			handler.startSpider(messenger, (StartSpider) message);
		} else if(message instanceof StopSpider) {
			handler.stopSpider(messenger, (StopSpider) message);
		} else if(message instanceof GetSpiderStatus) {
			handler.getSpiderStatus(messenger, (GetSpiderStatus) message);
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
