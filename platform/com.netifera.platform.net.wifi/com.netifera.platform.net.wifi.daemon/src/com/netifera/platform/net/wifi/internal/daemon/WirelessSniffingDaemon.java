package com.netifera.platform.net.wifi.internal.daemon;

import java.util.Collection;

import com.netifera.platform.api.dispatcher.IMessenger;
import com.netifera.platform.api.dispatcher.MessengerException;
import com.netifera.platform.net.daemon.sniffing.extend.AbstractSniffingDaemon;
import com.netifera.platform.net.daemon.sniffing.model.SniffingSessionEntity;
import com.netifera.platform.net.pcap.ICaptureInterface;
import com.netifera.platform.net.sniffing.ICaptureFileInterface;
import com.netifera.platform.net.sniffing.ISniffingEngineService;
import com.netifera.platform.net.wifi.daemon.IWirelessSniffingModule;
import com.netifera.platform.net.wifi.internal.daemon.remote.RequestWirelessModuleInformation;
import com.netifera.platform.net.wifi.internal.daemon.remote.SetWirelessModuleEnableState;
import com.netifera.platform.net.wifi.sniffing.IWifiSniffingEngine;

public class WirelessSniffingDaemon extends AbstractSniffingDaemon implements IWirelessSniffingDaemonMessageHandler {

	public final static String MESSAGE_PREFIX = "wifi";
	
	private final WirelessSniffingDaemonModules modules;
	private final WirelessSniffingMessageDispatcher messages;
	
	private IWifiSniffingEngine wifiSniffingEngine;
	
	public WirelessSniffingDaemon() {
		super(MESSAGE_PREFIX);

		modules = new WirelessSniffingDaemonModules(this);
		messages = new WirelessSniffingMessageDispatcher(this);
	}

	@Override
	protected void onActivate() {
		messages.registerHandlers(getDispatcher().getServerDispatcher());		
	}
	
	protected void registerWirelessModule(IWirelessSniffingModule module) {
		modules.addModule(module);
	}
	
	protected void unregisterWirelessModule(IWirelessSniffingModule module) {
		modules.removeModule(module);
	}
	
	protected void setWirelessSniffingEngine(IWifiSniffingEngine sniffingEngine) {
		wifiSniffingEngine = sniffingEngine;
	}
	
	protected void unsetWirelessSniffingEngine(IWifiSniffingEngine sniffingEngine) {
		
	}
	


	public void requestWirelessModuleInformation(IMessenger messenger,
			RequestWirelessModuleInformation message) throws MessengerException {
		modules.requestModuleInformation(messenger, message);		
	}


	public void setWirelessModuleEnableState(IMessenger messenger,
			SetWirelessModuleEnableState message) throws MessengerException {
		modules.setWirelessModuleEnableState(messenger, message);
	}

	
	@Override
	public void doStartCaptureFile(long spaceId, long realmId,
			ICaptureFileInterface captureFileInterface) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void doStop() {
		getModuleExecutionProvider().stopModules();
		modules.stopModules();
		
	}

	@Override
	public ISniffingEngineService getSniffingEngine() {
		return wifiSniffingEngine;
	}

	@Override
	protected void doStart(long spaceId,
			Collection<ICaptureInterface> enabledInterfaces) {
		final long realmId = getProbeManager().getLocalProbe().getEntity().getId();
		final SniffingSessionEntity sessionEntity = getSniffingEntityFactory().createSniffingSession(realmId, spaceId);
		getModuleExecutionProvider().startModules(enabledInterfaces, spaceId, sessionEntity.getId(), true);
		modules.startModules(enabledInterfaces, spaceId, sessionEntity.getId(), true);
		
	}



}
