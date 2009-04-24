package com.netifera.platform.net.http.internal.spider.daemon;

import java.util.HashMap;
import java.util.Map;

import com.netifera.platform.api.dispatcher.IClientDispatcher;
import com.netifera.platform.api.dispatcher.IMessageDispatcher;
import com.netifera.platform.api.dispatcher.IMessageDispatcherService;
import com.netifera.platform.api.events.IEventHandler;
import com.netifera.platform.api.log.ILogManager;
import com.netifera.platform.api.log.ILogger;
import com.netifera.platform.api.probe.IProbe;
import com.netifera.platform.net.http.internal.spider.daemon.remote.WebSpiderDaemonStub;
import com.netifera.platform.net.http.spider.daemon.IWebSpiderDaemon;
import com.netifera.platform.net.http.spider.daemon.IWebSpiderDaemonFactory;

public class WebSpiderDaemonFactory implements IWebSpiderDaemonFactory {

	private ILogger logger;
	private Map<IProbe, WebSpiderDaemonStub> probeMap = new HashMap<IProbe, WebSpiderDaemonStub>();
	private IClientDispatcher clientDispatcher;


	public IWebSpiderDaemon createForProbe(IProbe probe, IEventHandler changeHandler) {
		if(probeMap.containsKey(probe)) {
			return probeMap.get(probe);
		}
		WebSpiderDaemonStub rsd = new WebSpiderDaemonStub(probe, logger, changeHandler);
		probeMap.put(probe, rsd);
		return rsd;
		
	}
	
	public IWebSpiderDaemon lookupForProbe(IProbe probe) {
		return probeMap.get(probe);
	}
		
	private void registerHandlers(IMessageDispatcher dispatcher) {
/*		IMessageHandler handler = new IMessageHandler() {
			public void call(IMessenger messenger, IProbeMessage message)
					throws DispatchException {
				if (message instanceof CaptureFileProgress) {
					captureFileProgress(messenger, (CaptureFileProgress) message);
				} else if(message instanceof SniffingModuleOutput) {
					sniffingModuleOutput(messenger, (SniffingModuleOutput) message);
				} else {
					throw new DispatchMismatchException(message);
				}				
				
			}
		};
		dispatcher.registerMessageHandler(CaptureFileProgress.ID, handler);
		dispatcher.registerMessageHandler(SniffingModuleOutput.ID, handler);
*/
	}
	
	protected void setLogManager(ILogManager logManager) {
		logger = logManager.getLogger("Sniffing Daemon");
	}
	
	protected void unsetLogManager(ILogManager logManager) {
		logger = null;
	}
	
	protected void setDispatcher(IMessageDispatcherService dispatcher) {
		clientDispatcher = dispatcher.getClientDispatcher();
		registerHandlers(clientDispatcher);
	}
	
	protected void unsetDispatcher(IMessageDispatcherService dispatcher) {
		clientDispatcher = null;
	}
}
