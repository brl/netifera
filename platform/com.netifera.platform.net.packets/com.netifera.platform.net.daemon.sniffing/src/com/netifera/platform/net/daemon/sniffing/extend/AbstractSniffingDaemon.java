package com.netifera.platform.net.daemon.sniffing.extend;

import java.util.Collection;

import org.osgi.service.component.ComponentContext;

import com.netifera.platform.api.dispatcher.IMessageDispatcherService;
import com.netifera.platform.api.dispatcher.IMessenger;
import com.netifera.platform.api.dispatcher.MessengerException;
import com.netifera.platform.api.log.ILogManager;
import com.netifera.platform.api.log.ILogger;
import com.netifera.platform.api.probe.IProbeManagerService;
import com.netifera.platform.net.daemon.sniffing.model.CaptureFileEntity;
import com.netifera.platform.net.daemon.sniffing.model.ISniffingEntityFactory;
import com.netifera.platform.net.daemon.sniffing.model.NetworkInterfaceEntity;
import com.netifera.platform.net.daemon.sniffing.module.ISniffingModule;
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
import com.netifera.platform.net.internal.daemon.sniffing.SniffingDaemonCaptureFile;
import com.netifera.platform.net.internal.daemon.sniffing.SniffingDaemonInterfaces;
import com.netifera.platform.net.internal.daemon.sniffing.SniffingDaemonMessageDispatcher;
import com.netifera.platform.net.internal.daemon.sniffing.SniffingDaemonModules;
import com.netifera.platform.net.pcap.ICaptureInterface;
import com.netifera.platform.net.sniffing.ICaptureFileInterface;
import com.netifera.platform.net.sniffing.ISniffingEngineService;

abstract public class AbstractSniffingDaemon implements ISniffingDaemonMessageHandler {

	/* Sniffing Daemon subsystems */
	private final SniffingDaemonModules modules;
	private final SniffingDaemonMessageDispatcher messages;
	private final SniffingDaemonCaptureFile capture;
	private final SniffingDaemonInterfaces interfaces;

	private final String messagePrefix;

	private IMessenger activeMessenger;
	/* OSGi Services */
	private ILogger logger;
	private ISniffingEntityFactory entityFactory;
	private IProbeManagerService probeManager;
	private IMessageDispatcherService dispatcher;


	protected AbstractSniffingDaemon(String messagePrefix) {
		this.messagePrefix = messagePrefix;
		messages = new SniffingDaemonMessageDispatcher(this);
		modules = new SniffingDaemonModules(this);
		capture = new SniffingDaemonCaptureFile(this);
		interfaces = new SniffingDaemonInterfaces(this);
	}

	boolean isRunning() {
		return modules.isRunning() || capture.isRunning();
	}

	public String getMessagePrefix() {
		return messagePrefix;
	}

	public IMessenger getActiveMessenger() {
		return activeMessenger;
	}

	public void setActiveMessengerClosed() {
		activeMessenger = null;
	}

	public ILogger getLogger() {
		return logger;
	}

	protected IProbeManagerService getProbeManager() {
		return probeManager;
	}

	protected IMessageDispatcherService getDispatcher() {
		return dispatcher;
	}

	public ISniffingEntityFactory getSniffingEntityFactory() {
		return entityFactory;
	}

	protected IModuleExecutionProvider getModuleExecutionProvider() {
		return modules;
	}

	public NetworkInterfaceEntity createNetworkInterfaceEntity(long realmId, long spaceId, String interfaceName) {
		return entityFactory.createNetworkInterface(realmId, spaceId, interfaceName);
	}

	public CaptureFileEntity createCaptureFileEntity(long spaceId, String path) {
		final long realmId = probeManager.getLocalProbe().getEntity().getId();
		return entityFactory.createCaptureFile(realmId, spaceId, path);
	}

	protected void onActivate() { }
	abstract public ISniffingEngineService getSniffingEngine();
	abstract protected void doStart(long spaceId, Collection<ICaptureInterface> enabeledInterfaces);
	abstract public void doStartCaptureFile(long spaceId, long realmId, ICaptureFileInterface captureFileInterface);
	abstract public void doStop();

	/*
	 * OSGi DS binding
	 */

	protected void activate(ComponentContext ctx) {
		onActivate();
		interfaces.initialize();
		messages.registerHandlers(dispatcher.getServerDispatcher());
	}

	protected void deactivate(ComponentContext ctx) { }

	protected void registerModule(ISniffingModule module) {
		modules.addModule(module);
	}

	protected void unregisterModule(ISniffingModule module) {
		modules.removeModule(module);
	}

	protected void setProbeManager(IProbeManagerService manager) {
		this.probeManager = manager;
	}

	protected void unsetProbeManager(IProbeManagerService manager) {}

	protected void setDispatcher(IMessageDispatcherService dispatcher) {
		this.dispatcher = dispatcher;
	}

	protected void unsetDispatcher(IMessageDispatcherService dispatcher) {}

	protected void setLogManager(ILogManager logManager) {
		logger = logManager.getLogger("Sniffing Daemon");
	}

	protected void unsetLogManager(ILogManager logManager) {}

	protected void setEntityFactory(ISniffingEntityFactory factory) {
		this.entityFactory = factory;
	}

	protected void unsetEntityFactory(ISniffingEntityFactory factory) {}

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
		capture.captureFileValid(messenger, msg);
	}


	public void requestModuleInformation(IMessenger messenger,
			RequestModuleInformation msg) throws MessengerException {
		modules.requestModuleInformation(messenger, msg);
	}

	public void runCaptureFile(IMessenger messenger, RunCaptureFile msg)
			throws MessengerException {
		if(isRunning()) {
			messenger.respondError(msg, "Cannot run capture file as Sniffing Service is already running");
			return;
		}
		activeMessenger = messenger;
		capture.runCaptureFile(messenger, msg);
		messenger.respondOk(msg);
	}

	 public void setInterfaceEnableState(IMessenger messenger,
			SetInterfaceEnableState msg) throws MessengerException {
		 interfaces.setInterfaceEnableState(messenger, msg);
	 }
	 public void requestInterfaceInformation(IMessenger messenger, RequestInterfaceInformation msg) throws MessengerException {
		 interfaces.requestInterfaceInformation(messenger, msg);
	 }

	public void setModuleEnableState(IMessenger messenger,
			SetModuleEnableState msg) throws MessengerException {
		modules.setModuleEnableState(messenger, msg);
	}

	public void sniffingDaemonStatus(IMessenger messenger,
			SniffingDaemonStatus msg) throws MessengerException {
		messenger.emitMessage(msg.createResponse(messagePrefix, isRunning()));
	}

	public void startSniffingDaemon(IMessenger messenger,
			StartSniffingDaemon msg) throws MessengerException {
		if(isRunning()) {
			messenger.respondError(msg, "Sniffing Service already running");
		}
		activeMessenger = messenger;
		doStart(msg.getSpaceId(), interfaces.getEnabledInterfaces());
		messenger.respondOk(msg);
	}

	public void stopSniffingDaemon(IMessenger messenger, StopSniffingDaemon msg)
			throws MessengerException {
		doStop();
		messenger.respondOk(msg);
	}



}
