package com.netifera.platform.net.daemon.sniffing.extend;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import com.netifera.platform.api.dispatcher.IProbeMessage;
import com.netifera.platform.api.dispatcher.MessengerException;
import com.netifera.platform.api.events.EventListenerManager;
import com.netifera.platform.api.events.IEvent;
import com.netifera.platform.api.events.IEventHandler;
import com.netifera.platform.api.log.ILogger;
import com.netifera.platform.api.probe.IProbe;
import com.netifera.platform.dispatcher.StatusMessage;
import com.netifera.platform.net.daemon.sniffing.ISniffingDaemon;
import com.netifera.platform.net.daemon.sniffing.module.ISniffingModule;
import com.netifera.platform.net.internal.daemon.remote.CancelCaptureFile;
import com.netifera.platform.net.internal.daemon.remote.CaptureFileProgress;
import com.netifera.platform.net.internal.daemon.remote.CaptureFileStub;
import com.netifera.platform.net.internal.daemon.remote.CaptureFileValid;
import com.netifera.platform.net.internal.daemon.remote.InterfaceRecord;
import com.netifera.platform.net.internal.daemon.remote.ModuleRecord;
import com.netifera.platform.net.internal.daemon.remote.RequestInterfaceInformation;
import com.netifera.platform.net.internal.daemon.remote.RequestModuleInformation;
import com.netifera.platform.net.internal.daemon.remote.RunCaptureFile;
import com.netifera.platform.net.internal.daemon.remote.SetInterfaceEnableState;
import com.netifera.platform.net.internal.daemon.remote.SetModuleEnableState;
import com.netifera.platform.net.internal.daemon.remote.SniffingDaemonStatus;
import com.netifera.platform.net.internal.daemon.remote.StartSniffingDaemon;
import com.netifera.platform.net.internal.daemon.remote.StopSniffingDaemon;
import com.netifera.platform.net.pcap.ICaptureInterface;
import com.netifera.platform.net.sniffing.ICaptureFileInterface;
import com.netifera.platform.net.sniffing.util.ICaptureFileProgress;

abstract public class AbstractSniffingDaemonStub implements ISniffingDaemon {
	private final IProbe probe;
	private final ILogger logger;
	private String messengerError;
	private  final BlockingQueue<IProbeMessage> sendQueue;
	private final Thread sendThread;
	private ICaptureFileProgress progress;
	
	private final String messagePrefix;
	
	private List<InterfaceRecord> interfaceRecords;
	private List<ModuleRecord> moduleRecords;
	protected Object lock = new Object();
	
	private final EventListenerManager stateChangeListeners;
	
	private boolean isRunning;
	
	protected AbstractSniffingDaemonStub(String messagePrefix, IProbe probe, ILogger logger, IEventHandler changeHandler) {
		this.probe = probe;
		this.logger = logger;
		this.messagePrefix = messagePrefix;
		stateChangeListeners = new EventListenerManager();
		stateChangeListeners.addListener(changeHandler);
		
		sendQueue = new ArrayBlockingQueue<IProbeMessage>(10);
		sendThread = new Thread(createSendMessageRunnable());
		sendThread.start();
		refreshInterfaceInformation();
		refreshModuleInformation();
		refreshStatus();
	}
	
	public ICaptureFileInterface createCaptureFileInterface(String path) {
		final CaptureFileValid valid = (CaptureFileValid) exchangeMessage(new CaptureFileValid(messagePrefix, path));
		if(valid == null) {
			logger.warning("CaptureFileValid failed");
			return null;
		}
		return new CaptureFileStub(path, valid.isValid(), valid.getErrorMessage());
	}

	public void enableInterfaces(Collection<ICaptureInterface> interfaces) {
		final List<InterfaceRecord> interfaceRecords = new ArrayList<InterfaceRecord>();
		for(ICaptureInterface iface : interfaces) {
			interfaceRecords.add(new InterfaceRecord(iface.getName(), true, true));
		}
		sendQueue.add(new SetInterfaceEnableState(messagePrefix, interfaceRecords));
	}

	public void enableModules(Set<ISniffingModule> enabledModuleSet) {
		final List<ModuleRecord> moduleRecords = new ArrayList<ModuleRecord>();
		for(ISniffingModule module : enabledModuleSet) {
			moduleRecords.add(new ModuleRecord(module.getName(), true));
		}
		sendQueue.add(new SetModuleEnableState(messagePrefix, moduleRecords));		
	}

	public Collection<ICaptureInterface> getInterfaces() {
		synchronized(lock) {
			while(interfaceRecords == null) {
				try {
					lock.wait();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return Collections.emptyList();
				}
			}
			return new ArrayList<ICaptureInterface>(interfaceRecords);
		}
		
	}

	private List<InterfaceRecord> getInterfaceRecords() {
		final RequestInterfaceInformation response = (RequestInterfaceInformation) exchangeMessage(new RequestInterfaceInformation(messagePrefix));
		if(response == null) {
			logger.warning("Failed to get capture interface information: " + getLastError());
			return null;
		}
		return response.getInterfaceRecords();
		
	}
	public Set<ISniffingModule> getModules() {
		synchronized (lock) {
			while(moduleRecords == null) {
				try {
					lock.wait();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return Collections.emptySet();
				}
			}
			return new HashSet<ISniffingModule>(moduleRecords);
		}
	}

	private List<ModuleRecord> getModuleRecords() {
		final RequestModuleInformation response = (RequestModuleInformation) exchangeMessage(new RequestModuleInformation(messagePrefix));
		if(response == null) {
			logger.warning("Failed to get module information: " + getLastError());
			return null;
		}
		return response.getModuleRecords();
		
	}
	
	public boolean isEnabled(ICaptureInterface iface) {
		synchronized(lock) {
			while(interfaceRecords == null) {
				try {
					lock.wait();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return false;
				}
			}
			
			for(InterfaceRecord record : interfaceRecords) {
				if(record.getName().equals(iface.getName()))
					return record.isEnabled();
			}
			logger.warning("Interface not found for name " + iface.getName());
			return false;
		}
		
	
	}

	public boolean isEnabled(ISniffingModule module) {
		synchronized(lock) {
			while(moduleRecords == null) {
				try {
					lock.wait();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return false;
				}
			}
			
			for(ModuleRecord record : moduleRecords) {
				if(record.getName().equals(module.getName())) {
					return record.isEnabled();
				}
			}
			logger.warning("Module not found for name " + module.getName());
			return false;
		}
		
	}

	public void runCaptureFile(long spaceId, ICaptureFileInterface iface,
			ICaptureFileProgress progress) {
		this.progress = progress;
		if(!sendMessage(new RunCaptureFile(messagePrefix, spaceId, iface.getPath()))) {
			logger.warning("Error running capture file " + getLastError());
		}
	}
	
	public void cancelCaptureFile() {
		if(!sendMessage(new CancelCaptureFile(messagePrefix))) {
			logger.warning("Error cancelling capture file " + getLastError());
		}
	}

	public void setEnabled(ICaptureInterface iface, boolean enable) {
		final InterfaceRecord interfaceRecord = new InterfaceRecord(iface.getName(), iface.captureAvailable(), enable);
		sendQueue.add(new SetInterfaceEnableState(messagePrefix, interfaceRecord));
		refreshInterfaceInformation();
	}

	public void setEnabled(ISniffingModule module, boolean enable) {
		final ModuleRecord moduleRecord = new ModuleRecord(module.getName(), enable);
		sendQueue.add(new SetModuleEnableState(messagePrefix, moduleRecord));
		refreshModuleInformation();
	}

	public boolean isRunning() {
		return isRunning;
	}

	public void start(long spaceId) {
		sendQueue.add(new StartSniffingDaemon(messagePrefix, spaceId));
		isRunning = true;
		refreshStatus();
	}

	public void stop() {
		sendQueue.add(new StopSniffingDaemon(messagePrefix));
		isRunning = false;
		refreshStatus();		
	}
	
	public void captureFileProgress(CaptureFileProgress progressUpdate) {
		if(progressUpdate.isUpdate()) {
			progress.updateProgress(progressUpdate.getPercent(), progressUpdate.getCount());
		} else if(progressUpdate.isError()) {
			progress.error(progressUpdate.getErrorMessage(), null);
		} else {
			progress.finished();
		}
	}
	
	public void sniffingModuleOutput(String output) {
		logger.getManager().logRaw(output);
	}
	
	protected ILogger getLogger() {
		return logger;
	}
	protected String getLastError() {
		return messengerError;
	}
	
	protected void enqueue(IProbeMessage message) {
		sendQueue.add(message);
	}
	private boolean sendMessage(IProbeMessage message) {
		try {
			probe.getMessenger().sendMessage(message);
			return true;
		} catch (MessengerException e) {
			messengerError = e.getMessage();
			return false;
		} 
	}
	
	protected IProbeMessage exchangeMessage(IProbeMessage message) {
		try {
			IProbeMessage response = probe.getMessenger().exchangeMessage(message);
			if(response instanceof StatusMessage) { 
				return null;
			} else {
				return response;
			}
		} catch (MessengerException e) {
			messengerError = e.getMessage();
			return null;
		}
	}
	
	
	private Runnable createSendMessageRunnable() {
		return new Runnable() {

			public void run() {
				while(!Thread.interrupted()) {
					try {
						IProbeMessage message = sendQueue.take();
						
						if(!sendMessage(message)) {
							logger.error("failed to send message : " + messengerError);
						}
						synchronized (sendQueue) {
							sendQueue.notifyAll();
						}
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				}				
			}
			
		};
	}
	
	protected void waitForEmptySendQueue() {
		synchronized (sendQueue) {
			while(!sendQueue.isEmpty()) {
				try {
					sendQueue.wait();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
			}
		}
	}
	private void refreshInterfaceInformation() {
		Thread t = new Thread(new Runnable() {
			public void run() {
				waitForEmptySendQueue();
				synchronized(lock) {
				interfaceRecords = getInterfaceRecords();
				lock.notifyAll();
				}
			}
		});
		t.start();
	}
	
	private void refreshModuleInformation() {
		Thread t = new Thread(new Runnable() {
			public void run() {
				waitForEmptySendQueue();
				synchronized(lock) {
					moduleRecords = getModuleRecords();
					lock.notifyAll();
				}
			}
		});
		t.start();
	}
	
	private void refreshStatus() {
		Thread t = new Thread(new Runnable() {

			public void run() {
				waitForEmptySendQueue();
				SniffingDaemonStatus status = (SniffingDaemonStatus) exchangeMessage(new SniffingDaemonStatus(messagePrefix));
				if(status == null) {
					logger.warning("Failed to receive status message");
					return;
				}
				if(status.isRunning() != isRunning) {
					isRunning = status.isRunning();
					stateChangeListeners.fireEvent(new IEvent() {});
				}
				
			}
			
		});
		t.start();
		
	}
	
	
	
}
