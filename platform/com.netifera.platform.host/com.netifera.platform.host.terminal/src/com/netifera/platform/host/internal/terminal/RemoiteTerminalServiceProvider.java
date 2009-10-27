package com.netifera.platform.host.internal.terminal;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import com.netifera.platform.api.dispatcher.DispatchException;
import com.netifera.platform.api.dispatcher.DispatchMismatchException;
import com.netifera.platform.api.dispatcher.IMessageDispatcher;
import com.netifera.platform.api.dispatcher.IMessageDispatcherService;
import com.netifera.platform.api.dispatcher.IMessageHandler;
import com.netifera.platform.api.dispatcher.IMessenger;
import com.netifera.platform.api.dispatcher.IProbeMessage;
import com.netifera.platform.api.log.ILogManager;
import com.netifera.platform.api.log.ILogger;
import com.netifera.platform.api.probe.IProbe;
import com.netifera.platform.host.terminal.ITerminalService;
import com.netifera.platform.host.terminal.probe.RemoteTerminalService;
import com.netifera.platform.host.terminal.probe.TerminalClosed;
import com.netifera.platform.host.terminal.probe.TerminalOutput;
import com.netifera.platform.services.IRemoteServiceProvider;

public class RemoiteTerminalServiceProvider implements IRemoteServiceProvider {

	private ILogger logger;
	private Map<IProbe, RemoteTerminalService> probeMap =
		new HashMap<IProbe, RemoteTerminalService>();

	public Object create(URI url, IProbe probe) {
		//XXX ignoring url
		if(probeMap.containsKey(probe))
			return probeMap.get(probe);
		
		final RemoteTerminalService rtm = new RemoteTerminalService(probe, logger);
		probeMap.put(probe, rtm);
		return rtm;
	}

	public Class<?> getType() {
		return ITerminalService.class;
	}

	private void registerHandlers(IMessageDispatcher dispatcher) {
		IMessageHandler handler = new IMessageHandler() {

			public void call(IMessenger messenger, IProbeMessage message)
					throws DispatchException {
				RemoteTerminalService rtm = (RemoteTerminalService) probeMap.get(messenger.getProbe());
				if(message instanceof TerminalOutput) {
					rtm.terminalOutput((TerminalOutput) message);
				} else if(message instanceof TerminalClosed) {
					rtm.terminalClosed((TerminalClosed) message);
				} else {
					throw new DispatchMismatchException(message);
				}				
			}
		};
		dispatcher.registerMessageHandler(TerminalOutput.ID, handler);
		dispatcher.registerMessageHandler(TerminalClosed.ID, handler);
	}
	
	protected void setMessageDispatcher(IMessageDispatcherService dispatcher) {
		registerHandlers(dispatcher.getClientDispatcher());
	}
	
	protected void unsetMessageDispatcher(IMessageDispatcherService dispatcher) {
	}
	
	protected void setLogManager(ILogManager logManager) {
		logger = logManager.getLogger("Terminal Service");
	}
	
	protected void unsetLogManager(ILogManager logManager) {
	}
}
