package com.netifera.platform.net.daemon.sniffing.extend;

import java.util.ArrayList;
import java.util.List;

import com.netifera.platform.api.dispatcher.IMessageDispatcherService;
import com.netifera.platform.api.dispatcher.IMessenger;
import com.netifera.platform.api.dispatcher.MessengerException;
import com.netifera.platform.api.log.ILogManager;
import com.netifera.platform.api.log.ILogger;
import com.netifera.platform.api.probe.IProbeManagerService;
import com.netifera.platform.net.daemon.sniffing.ISniffingModule;
import com.netifera.platform.net.daemon.sniffing.model.ISniffingEntityFactory;
import com.netifera.platform.net.daemon.sniffing.model.SniffingSessionEntity;
import com.netifera.platform.net.internal.daemon.probe.CancelCaptureFile;
import com.netifera.platform.net.internal.daemon.probe.CaptureFileValid;
import com.netifera.platform.net.internal.daemon.probe.InterfaceRecord;
import com.netifera.platform.net.internal.daemon.probe.ModuleRecord;
import com.netifera.platform.net.internal.daemon.probe.RequestInterfaceInformation;
import com.netifera.platform.net.internal.daemon.probe.RequestModuleInformation;
import com.netifera.platform.net.internal.daemon.probe.RunCaptureFile;
import com.netifera.platform.net.internal.daemon.probe.SetInterfaceEnableState;
import com.netifera.platform.net.internal.daemon.probe.SetModuleEnableState;
import com.netifera.platform.net.internal.daemon.probe.SniffingDaemonStatus;
import com.netifera.platform.net.internal.daemon.probe.StartSniffingDaemon;
import com.netifera.platform.net.internal.daemon.probe.StopSniffingDaemon;
import com.netifera.platform.net.pcap.ICaptureInterface;
import com.netifera.platform.net.sniffing.ICaptureFileInterface;
import com.netifera.platform.net.sniffing.ISniffingEngineService;

public class AbstractSniffingDaemon implements ISniffingDaemonMessageHandler {
	
	private final SniffingDaemonModules modules = new SniffingDaemonModules();
	private final SniffingDaemonMessageDispatcher messages = new SniffingDaemonMessageDispatcher(this);
	private final SniffingDaemonInterfaces interfaces = new SniffingDaemonInterfaces();
	private final SniffingDaemonCaptureFile capture = new SniffingDaemonCaptureFile(modules);
	
	
	private ILogger logger;
	private IMessenger openMessenger;
	private ISniffingEngineService sniffingEngine;
	private ISniffingEntityFactory entityFactory;
	private IProbeManagerService probeManager;
	
	
	boolean isRunning() {
		return modules.isRunning();
	}
	
	
	public void start(IMessenger messenger, long spaceId) {
		System.out.println("start go go");
		final long realmId = probeManager.getLocalProbe().getEntity().getId();
		final SniffingSessionEntity session = entityFactory.createSniffingSession(realmId, spaceId);
		
		modules.start(messenger, sniffingEngine, interfaces.getEnabledInterfaces(), spaceId, session.getId());
		

	}
	
	void stop() {
		
	}
	
	/*
	 * OSGi DS bindings
	 */
	protected void registerModule(ISniffingModule module) {
		modules.addModule(module);
	}

	protected void unregisterModule(ISniffingModule module) {
		modules.removeModule(module);
	}
	
	protected void setSniffingEngine(ISniffingEngineService sniffingEngine) {
		this.sniffingEngine = sniffingEngine;
		interfaces.setSniffingEngine(sniffingEngine);
	}
	
	protected void unsetSniffingEngine(ISniffingEngineService sniffingEngine) {
		
	}
	
	protected void setProbeManager(IProbeManagerService manager) {
		this.probeManager = manager;;
	}
	
	protected void unsetProbeManager(IProbeManagerService manager) {
		
	}
	
	protected void setDispatcher(IMessageDispatcherService dispatcherService) {
		messages.registerHandlers(dispatcherService.getServerDispatcher());
	}
	
	protected void setLogManager(ILogManager logManager) {
		logger = logManager.getLogger("Sniffing Daemon");
		messages.setLogger(logger);
		modules.setLogger(logger);
	}
	
	protected void unsetLogManager(ILogManager logManager) {
		
	}
	
	protected void setEntityFactory(ISniffingEntityFactory factory) {
		this.entityFactory = factory;
		modules.setEntityFactory(factory);
	}
	
	protected void unsetEntityFactory(ISniffingEntityFactory factory) {
		
	}
	
	/*
	 * Probe message handling
	 */
	public void cancelCaptureFile(IMessenger messenger, CancelCaptureFile msg)
			throws MessengerException {
		capture.cancelCaptureFile();
		messenger.respondOk(msg);
	}
	
	public void captureFileValid(IMessenger messenger, CaptureFileValid msg)
			throws MessengerException {
		final ICaptureFileInterface iface = capture.createCaptureFileInterface(msg.getPath());
		messenger.emitMessage(msg.createResponse(iface.isValid(), iface.getErrorMessage()));
		iface.dispose();		
	}
	
	public void requestInterfaceInformation(IMessenger messenger,
			RequestInterfaceInformation msg) throws MessengerException {
		final List<InterfaceRecord> result = new ArrayList<InterfaceRecord>();
		for(ICaptureInterface iface : interfaces.getInterfaces())
			result.add(new InterfaceRecord(iface.getName(), iface.toString(), iface.captureAvailable(), interfaces.isEnabled(iface)));
		messenger.emitMessage(msg.createResponse(result));
		
	}
	  
	public void requestModuleInformation(IMessenger messenger,
			RequestModuleInformation msg) throws MessengerException {
		final List <ModuleRecord> result = new ArrayList<ModuleRecord>();
		for(ISniffingModule mod : modules.getModules()) {
			result.add(new ModuleRecord(mod.getName(), modules.isEnabled(mod)));
		}
		messenger.emitMessage(msg.createResponse(result));		
	}
	
	public void runCaptureFile(IMessenger messenger, RunCaptureFile msg)
			throws MessengerException {
		// TODO Auto-generated method stub
		
	}
	public void setInterfaceEnableState(IMessenger messenger,
			SetInterfaceEnableState msg) throws MessengerException {
		for(InterfaceRecord iface : msg.getInterfaceRecords()) {
			final ICaptureInterface captureInterface = interfaces.lookupInterfaceByName(iface.getName());
			if(captureInterface == null) {
				logger.warning("No capture interface found with name : " + iface.getName());
			} else {
				if(iface.isEnabled()) {
					interfaces.enableInterface(iface);
				} else {
					interfaces.disableInterface(iface);
				}
			}
		}
		messenger.respondOk(msg);		
	}
	public void setModuleEnableState(IMessenger messenger,
			SetModuleEnableState msg) throws MessengerException {
		for(ModuleRecord module : msg.getModuleRecords()) {
			final ISniffingModule sniffingModule = modules.getModuleByName(module.getName());
			if(sniffingModule == null) {
				logger.warning("No sniffing module found with name : " + module.getName());
			} else {
				if(module.isEnabled())
					modules.enableModule(sniffingModule);
				else
					modules.disableModule(sniffingModule);
			}
		}
		messenger.respondOk(msg);
		
	}
	
	public void sniffingDaemonStatus(IMessenger messenger,
			SniffingDaemonStatus msg) throws MessengerException {
		messenger.emitMessage(msg.createResponse(isRunning()));
	}
	
	public void startSniffingDaemon(IMessenger messenger,
			StartSniffingDaemon msg) throws MessengerException {
		start(messenger, msg.getSpaceId());
		messenger.respondOk(msg);
		
	}
	public void stopSniffingDaemon(IMessenger messenger, StopSniffingDaemon msg)
			throws MessengerException {
		stop();
		messenger.respondOk(msg);
		
	}
	


}
