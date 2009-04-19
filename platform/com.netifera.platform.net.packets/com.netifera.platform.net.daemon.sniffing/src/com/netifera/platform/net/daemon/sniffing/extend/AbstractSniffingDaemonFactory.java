package com.netifera.platform.net.daemon.sniffing.extend;

import java.util.HashMap;
import java.util.Map;

import com.netifera.platform.api.dispatcher.DispatchException;
import com.netifera.platform.api.dispatcher.DispatchMismatchException;
import com.netifera.platform.api.dispatcher.IClientDispatcher;
import com.netifera.platform.api.dispatcher.IMessageDispatcher;
import com.netifera.platform.api.dispatcher.IMessageDispatcherService;
import com.netifera.platform.api.dispatcher.IMessageHandler;
import com.netifera.platform.api.dispatcher.IMessenger;
import com.netifera.platform.api.dispatcher.IProbeMessage;
import com.netifera.platform.api.events.IEventHandler;
import com.netifera.platform.api.log.ILogManager;
import com.netifera.platform.api.log.ILogger;
import com.netifera.platform.api.probe.IProbe;
import com.netifera.platform.net.daemon.sniffing.ISniffingDaemon;
import com.netifera.platform.net.daemon.sniffing.ISniffingDaemonFactory;
import com.netifera.platform.net.internal.daemon.remote.CaptureFileProgress;
import com.netifera.platform.net.internal.daemon.remote.SniffingModuleOutput;

abstract public class AbstractSniffingDaemonFactory implements ISniffingDaemonFactory {
	private ILogger logger;
	private Map<IProbe, AbstractSniffingDaemonStub> probeMap = new HashMap<IProbe, AbstractSniffingDaemonStub>();
	private IClientDispatcher clientDispatcher;
	private final String messagePrefix;

	protected AbstractSniffingDaemonFactory(String messagePrefix) {
		this.messagePrefix = messagePrefix;
	}

	abstract protected AbstractSniffingDaemonStub createStub(IProbe probe, IEventHandler changeHandler);
	
	public ISniffingDaemon createForProbe(IProbe probe, IEventHandler changeHandler) {
		if(probeMap.containsKey(probe)) {
			return probeMap.get(probe);
		}
		final AbstractSniffingDaemonStub stub = createStub(probe, changeHandler);
		probeMap.put(probe, stub);
		return stub;
	}
	
	public ISniffingDaemon lookupForProbe(IProbe probe) {
		return probeMap.get(probe);
	}
	
	protected ILogger getLogger() {
		return logger;
	}
	private void captureFileProgress(IMessenger messenger, CaptureFileProgress msg) {
		final AbstractSniffingDaemonStub stub = (AbstractSniffingDaemonStub) probeMap.get(messenger.getProbe());
		if(stub != null) {
			stub.captureFileProgress(msg);
		}
	}
	
	private void sniffingModuleOutput(IMessenger messenger, SniffingModuleOutput msg) {
		final AbstractSniffingDaemonStub stub = probeMap.get(messenger.getProbe());
		if(stub != null) {
			stub.sniffingModuleOutput(msg.getMessage());
		}
	}
	
	private void registerHandlers(IMessageDispatcher dispatcher) {
		IMessageHandler handler = new IMessageHandler() {

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
		dispatcher.registerMessageHandler(messagePrefix + CaptureFileProgress.ID, handler);
		dispatcher.registerMessageHandler(messagePrefix + SniffingModuleOutput.ID, handler);

	}
	
	protected void setLogManager(ILogManager logManager) {
		logger = logManager.getLogger("Sniffing Daemon [" + messagePrefix + "]");
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
