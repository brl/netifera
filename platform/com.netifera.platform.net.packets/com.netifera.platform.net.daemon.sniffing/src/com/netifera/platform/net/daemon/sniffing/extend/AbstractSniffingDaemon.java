package com.netifera.platform.net.daemon.sniffing.extend;

import org.osgi.service.component.ComponentContext;

import com.netifera.platform.api.dispatcher.IMessageDispatcherService;
import com.netifera.platform.api.dispatcher.IMessenger;
import com.netifera.platform.api.dispatcher.MessengerException;
import com.netifera.platform.api.log.ILogManager;
import com.netifera.platform.api.log.ILogger;
import com.netifera.platform.api.probe.IProbeManagerService;
import com.netifera.platform.net.daemon.sniffing.model.ISniffingEntityFactory;
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
import com.netifera.platform.net.sniffing.ISniffingEngineService;

public class AbstractSniffingDaemon implements ISniffingDaemonMessageHandler {

	/* Sniffing Daemon subsystems */
	private final SniffingDaemonModules modules;
	private final SniffingDaemonMessageDispatcher messages;
	private final SniffingDaemonInterfaces interfaces;
	private final SniffingDaemonCaptureFile capture;

	/* OSGi Services */
	private ILogger logger;
	private ISniffingEngineService sniffingEngine;
	private ISniffingEntityFactory entityFactory;
	private IProbeManagerService probeManager;
	private IMessageDispatcherService dispatcher;


	protected AbstractSniffingDaemon() {
		messages = new SniffingDaemonMessageDispatcher(this);
		interfaces = new SniffingDaemonInterfaces();
		modules = new SniffingDaemonModules(interfaces);
		capture = new SniffingDaemonCaptureFile(modules);
	}

	boolean isRunning() {
		return modules.isRunning() || capture.isRunning();
	}

	/*
	 * OSGi DS binding
	 */

	protected void activate(ComponentContext ctx) {
		interfaces.setServices(logger, sniffingEngine);
		capture.setServices(logger, entityFactory, probeManager, sniffingEngine);
		modules.setServices(logger, entityFactory, probeManager, sniffingEngine);
		messages.setServices(logger, dispatcher.getServerDispatcher());
		messages.registerHandlers();
	}

	protected void deactivate(ComponentContext ctx) { }

	protected void registerModule(ISniffingModule module) {
		modules.addModule(module);
	}

	protected void unregisterModule(ISniffingModule module) {
		modules.removeModule(module);
	}

	protected void setSniffingEngine(ISniffingEngineService sniffingEngine) {
		this.sniffingEngine = sniffingEngine;
	}

	protected void unsetSniffingEngine(ISniffingEngineService sniffingEngine) {}

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

	public void requestInterfaceInformation(IMessenger messenger,
			RequestInterfaceInformation msg) throws MessengerException {
		interfaces.requestInterfaceInformation(messenger, msg);
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
		capture.runCaptureFile(messenger, msg);
		messenger.respondOk(msg);
	}

	public void setInterfaceEnableState(IMessenger messenger,
			SetInterfaceEnableState msg) throws MessengerException {
		interfaces.setInterfaceEnableState(messenger, msg);
	}

	public void setModuleEnableState(IMessenger messenger,
			SetModuleEnableState msg) throws MessengerException {
		modules.setModuleEnableState(messenger, msg);
	}

	public void sniffingDaemonStatus(IMessenger messenger,
			SniffingDaemonStatus msg) throws MessengerException {
		messenger.emitMessage(msg.createResponse(isRunning()));
	}

	public void startSniffingDaemon(IMessenger messenger,
			StartSniffingDaemon msg) throws MessengerException {
		if(isRunning()) {
			messenger.respondError(msg, "Sniffing Service already running");
		}
		modules.startSniffingDaemon(messenger, msg);
	}

	public void stopSniffingDaemon(IMessenger messenger, StopSniffingDaemon msg)
			throws MessengerException {
		modules.stopSniffingDaemon(messenger, msg);
	}



}
