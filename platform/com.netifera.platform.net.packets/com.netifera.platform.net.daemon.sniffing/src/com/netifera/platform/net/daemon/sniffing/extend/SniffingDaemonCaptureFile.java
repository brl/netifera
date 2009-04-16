package com.netifera.platform.net.daemon.sniffing.extend;

import java.util.HashSet;
import java.util.Set;

import com.netifera.platform.api.dispatcher.IMessenger;
import com.netifera.platform.api.dispatcher.MessengerException;
import com.netifera.platform.api.probe.IProbeManagerService;
import com.netifera.platform.net.daemon.sniffing.model.CaptureFileEntity;
import com.netifera.platform.net.daemon.sniffing.model.ISniffingEntityFactory;
import com.netifera.platform.net.internal.daemon.probe.CaptureFileProgress;
import com.netifera.platform.net.internal.daemon.sniffing.CaptureFileRunnable;
import com.netifera.platform.net.internal.daemon.sniffing.SniffingDaemonInterface;
import com.netifera.platform.net.sniffing.ICaptureFileInterface;
import com.netifera.platform.net.sniffing.util.ICaptureFileProgress;

public class SniffingDaemonCaptureFile {
	private final static long SEND_INTERVAL = 20;
	
	private final SniffingDaemonModules modules;
	private IProbeManagerService probeManager;
	private ISniffingEntityFactory entityFactory;
	
	private ICaptureFileInterface runningCaptureFile;
	private IMessenger openMessenger;
	
	
	public SniffingDaemonCaptureFile(SniffingDaemonModules modules) {
		this.modules = modules;
	}
	
	ICaptureFileInterface createCaptureFileInterface(String path) {
		return null;
	}

	void runCaptureFile(ICaptureFileInterface iface, final ICaptureFileProgress progress, long spaceId) {
		if(!iface.isValid()) {
			progress.error("Invalid interface : " + iface.getName(), null);
			return;
		}
		
		final long realmId = probeManager.getLocalProbe().getEntity().getId();
		final CaptureFileEntity captureFile = entityFactory.createCaptureFile(realmId, spaceId, iface.getPath());
		final Set<SniffingDaemonInterface> interfaces = new HashSet<SniffingDaemonInterface>();
		interfaces.add(new SniffingDaemonInterface(iface, captureFile.getId()));
	
		
		//startModules(interfaces);
		// XXX start modules
		runningCaptureFile = iface;
		iface.process(createProgress());
	}
	
	void cancelCaptureFile() {
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
				stopModules();
				sendMessage(CaptureFileProgress.createError(message));				
			}

			public void finished() {
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
