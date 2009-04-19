package com.netifera.platform.net.internal.daemon.sniffing;

import com.netifera.platform.api.dispatcher.IMessenger;
import com.netifera.platform.api.dispatcher.MessengerException;
import com.netifera.platform.net.daemon.sniffing.extend.AbstractSniffingDaemon;
import com.netifera.platform.net.daemon.sniffing.model.CaptureFileEntity;
import com.netifera.platform.net.internal.daemon.remote.CaptureFileProgress;
import com.netifera.platform.net.internal.daemon.remote.CaptureFileValid;
import com.netifera.platform.net.internal.daemon.remote.RunCaptureFile;
import com.netifera.platform.net.sniffing.ICaptureFileInterface;
import com.netifera.platform.net.sniffing.util.ICaptureFileProgress;

public class SniffingDaemonCaptureFile {
	private final static long SEND_INTERVAL = 20;
		
	
	private final AbstractSniffingDaemon sniffingDaemon;
	private ICaptureFileInterface runningCaptureFile;
	
	private volatile boolean running;
	
	
	public SniffingDaemonCaptureFile(AbstractSniffingDaemon sniffingDaemon) {
		this.sniffingDaemon = sniffingDaemon;
	}
	
	public void runCaptureFile(IMessenger messenger, RunCaptureFile msg) throws MessengerException {
		ICaptureFileInterface iface = createCaptureFileInterface(msg.getPath());
		if(!iface.isValid()) {
			messenger.respondError(msg, iface.getErrorMessage());
			return;
		}
		final CaptureFileEntity captureFile = sniffingDaemon.createCaptureFileEntity(msg.getSpaceId(), msg.getPath());
		sniffingDaemon.doStartCaptureFile(msg.getSpaceId(), captureFile.getId(), iface);
	
		runningCaptureFile = iface;
		running = true;
		iface.process(createProgress());
	}
	
	public void captureFileValid(IMessenger messenger, CaptureFileValid msg) throws MessengerException {
		final ICaptureFileInterface iface = createCaptureFileInterface(msg.getPath());
		messenger.emitMessage(msg.createResponse(sniffingDaemon.getMessagePrefix(), iface.isValid(), iface.getErrorMessage()));
		iface.dispose();
	}
	
	public boolean isRunning() {
		return running;
	}
	
	ICaptureFileInterface createCaptureFileInterface(String path) {
		return sniffingDaemon.getSniffingEngine().createCaptureFileInterface(path);
	}

	
	public void cancelCaptureFile() {
		if(runningCaptureFile != null) {
			runningCaptureFile.cancelProcessing();
		}
	}
	
	private void stopModules() {
		sniffingDaemon.doStop();
		runningCaptureFile = null;
	}
	
	private ICaptureFileProgress createProgress() {
		return new ICaptureFileProgress() {
			private long messageCounter = 0;
			
			public void error(String message, Throwable e) {
				running = false;
				stopModules();
				sendMessage(CaptureFileProgress.createError(sniffingDaemon.getMessagePrefix(), message));				
			}

			public void finished() {
				running = false;
				stopModules();
				sendMessage(CaptureFileProgress.createFinished(sniffingDaemon.getMessagePrefix()));				
			}

			public boolean updateProgress(int percent, int count) {
				messageCounter++;
				if(messageCounter % SEND_INTERVAL == 0) {
					sendMessage(CaptureFileProgress.createUpdate(sniffingDaemon.getMessagePrefix(), percent, count));
				}
				return true;
			}
			
		};
	}
	
	private void sendMessage(CaptureFileProgress progressMessage) {
		if(sniffingDaemon.getActiveMessenger() == null)
			return;
		try {
			sniffingDaemon.getActiveMessenger().emitMessage(progressMessage);
		} catch(MessengerException e) {
			sniffingDaemon.setActiveMessengerClosed();
			Thread.currentThread().interrupt();
		}
	}
}
