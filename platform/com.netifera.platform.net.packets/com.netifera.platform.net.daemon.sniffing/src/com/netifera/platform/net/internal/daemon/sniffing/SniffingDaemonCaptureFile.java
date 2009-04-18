package com.netifera.platform.net.internal.daemon.sniffing;

import java.util.HashSet;
import java.util.Set;

import com.netifera.platform.api.dispatcher.IMessenger;
import com.netifera.platform.api.dispatcher.MessengerException;
import com.netifera.platform.api.log.ILogger;
import com.netifera.platform.api.probe.IProbeManagerService;
import com.netifera.platform.net.daemon.sniffing.model.CaptureFileEntity;
import com.netifera.platform.net.daemon.sniffing.model.ISniffingEntityFactory;
import com.netifera.platform.net.internal.daemon.remote.CaptureFileProgress;
import com.netifera.platform.net.internal.daemon.remote.CaptureFileValid;
import com.netifera.platform.net.internal.daemon.remote.RunCaptureFile;
import com.netifera.platform.net.sniffing.ICaptureFileInterface;
import com.netifera.platform.net.sniffing.ISniffingEngineService;
import com.netifera.platform.net.sniffing.util.ICaptureFileProgress;

public class SniffingDaemonCaptureFile {
	private final static long SEND_INTERVAL = 20;
	
	private final SniffingDaemonModules modules;
	
	private IProbeManagerService probeManager;
	private ISniffingEntityFactory entityFactory;
	private ISniffingEngineService sniffingEngine;
	private boolean isInitialized;
	
	private ICaptureFileInterface runningCaptureFile;
	private IMessenger openMessenger;
	
	private volatile boolean running;
	
	
	public SniffingDaemonCaptureFile(SniffingDaemonModules modules) {
		this.modules = modules;
	}
	
	public void setServices(ILogger logger, ISniffingEntityFactory factory, IProbeManagerService probeManager, ISniffingEngineService sniffingEngine) {
		this.entityFactory = factory;
		this.probeManager = probeManager;
		this.sniffingEngine = sniffingEngine;
		isInitialized = true;
		
	}
	public void runCaptureFile(IMessenger messenger, RunCaptureFile msg) throws MessengerException {
		verifyInitialized();
		ICaptureFileInterface iface = createCaptureFileInterface(msg.getPath());
		if(!iface.isValid()) {
			messenger.respondError(msg, iface.getErrorMessage());
			return;
		}
		openMessenger = messenger;
		final long realmId = probeManager.getLocalProbe().getEntity().getId();
		final CaptureFileEntity captureFile = entityFactory.createCaptureFile(realmId, msg.getSpaceId(), iface.getPath());
		final Set<SniffingDaemonInterface> interfaces = new HashSet<SniffingDaemonInterface>();
		interfaces.add(new SniffingDaemonInterface(iface, captureFile.getId()));
	
		for(EnabledSniffingModule enabledModule : modules.getEnabledModules()) {
			enabledModule.start(sniffingEngine, interfaces, msg.getSpaceId());
		}
		
		runningCaptureFile = iface;
		running = true;
		iface.process(createProgress());
	}
	
	public void captureFileValid(IMessenger messenger, CaptureFileValid msg) throws MessengerException {
		verifyInitialized();
		final ICaptureFileInterface iface = createCaptureFileInterface(msg.getPath());
		messenger.emitMessage(msg.createResponse(iface.isValid(), iface.getErrorMessage()));
		iface.dispose();
	}
	public boolean isRunning() {
		return running;
	}
	
	private void verifyInitialized() {
		if(!isInitialized) {
			throw new IllegalStateException("Sniffing Daemon Capture File subsystem is not initialized");
		}
	}
	
	ICaptureFileInterface createCaptureFileInterface(String path) {
		return sniffingEngine.createCaptureFileInterface(path);
	}

	
	public void cancelCaptureFile() {
		if(runningCaptureFile != null) {
			runningCaptureFile.cancelProcessing();
		}
	}
	
	
	private void stopModules() {
		runningCaptureFile = null;
		
	}
	
	private ICaptureFileProgress createProgress() {
		return new ICaptureFileProgress() {
			private long messageCounter = 0;
			
			public void error(String message, Throwable e) {
				running = false;
				stopModules();
				sendMessage(CaptureFileProgress.createError(message));				
			}

			public void finished() {
				running = false;
				stopModules();
				sendMessage(CaptureFileProgress.createFinished());				
			}

			public boolean updateProgress(int percent, int count) {
				messageCounter++;
				if(messageCounter % SEND_INTERVAL == 0) {
					sendMessage(CaptureFileProgress.createUpdate(percent, count));
				}
				return true;
			}
			
		};
	}
	
	private void sendMessage(CaptureFileProgress progressMessage) {
		if(openMessenger == null)
			return;
		try {
			openMessenger.emitMessage(progressMessage);
		} catch(MessengerException e) {
			openMessenger = null;
			Thread.currentThread().interrupt();
		}
	}
}
