package com.netifera.platform.net.http.internal.spider.daemon.remote;

import java.net.URI;
import java.util.Collections;
import java.util.Map;
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
import com.netifera.platform.net.http.spider.daemon.IWebSpiderDaemon;
import com.netifera.platform.net.http.spider.impl.WebSite;

public class WebSpiderDaemonStub implements IWebSpiderDaemon {
	private final IProbe probe;
	private final ILogger logger;
	private String messengerError;
	private  final BlockingQueue<IProbeMessage> sendQueue;
	private final Thread sendThread;
	
	private Set<String> availableModules;
	private WebSpiderConfiguration configuration;
	private Object lock = new Object();
	
	private final EventListenerManager stateChangeListeners;
	
	private boolean isRunning;
	
	public WebSpiderDaemonStub(IProbe probe, ILogger logger, IEventHandler changeHandler) {
		this.probe = probe;
		this.logger = logger;
		stateChangeListeners = new EventListenerManager();
		stateChangeListeners.addListener(changeHandler);
		
		sendQueue = new ArrayBlockingQueue<IProbeMessage>(10);
		sendThread = new Thread(createSendMessageRunnable());
		sendThread.start();
		refreshAvailableModules();
		refreshConfiguration();
		refreshStatus();
	}
	
	public Set<String> getAvailableModules() {
		synchronized(lock) {
			while(availableModules == null) {
				try {
					lock.wait();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return Collections.emptySet();
				}
			}
			
			return availableModules;
		}
	}

	public WebSpiderConfiguration getConfiguration() {
		synchronized(lock) {
			while(configuration == null) {
				try {
					lock.wait();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return null;
				}
			}

			return configuration;
		}
	}

	public void setConfiguration(WebSpiderConfiguration configuration) {
		this.configuration = configuration;
		sendQueue.add(new SetSpiderConfiguration(configuration));	
	}
	
	public boolean isEnabled(String moduleName) {
		synchronized(lock) {
			while(configuration == null) {
				try {
					lock.wait();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return false;
				}
			}
			
			return configuration.modules.contains(moduleName);
		}
	}

	public void setEnabled(String moduleName, boolean enable) {
		synchronized(lock) {
			while(configuration == null) {
				try {
					lock.wait();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
			}

			if (enable)
				configuration.modules.add(moduleName);
			else
				configuration.modules.remove(moduleName);
			
			setConfiguration(configuration);
		}
		
		refreshConfiguration();
	}

	public boolean isEnabled(WebSite site) {
		synchronized(lock) {
			while(configuration == null) {
				try {
					lock.wait();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return false;
				}
			}
			
			return configuration.targets.contains(site);
		}
	}

	public void setEnabled(WebSite site, boolean enable) {
		synchronized(lock) {
			while(configuration == null) {
				try {
					lock.wait();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
			}

			if (enable)
				configuration.targets.add(site);
			else
				configuration.targets.remove(site);
			
			setConfiguration(configuration);
		}
		
		refreshConfiguration();
	}

	public boolean isRunning() {
		return isRunning;
	}

	public void start(long spaceId) {
		sendQueue.add(new StartSpider(spaceId));
		isRunning = true;
		refreshStatus();
	}
	
	public void stop() {
		sendQueue.add(new StopSpider());
		isRunning = false;
		refreshStatus();		
	}

	public synchronized void fetch(URI url, String method, Map<String,String> headers, String content) {
		sendQueue.add(new FetchURL(url, method, headers, content));
	}

	public synchronized void visit(URI url) {
		sendQueue.add(new VisitURL(url));
	}
	
	private String getLastError() {
		return messengerError;
	}
	
	private boolean sendMessage(IProbeMessage message) {
		try {
			probe.getMessenger().sendMessage(message);
			return true;
		} catch (MessengerException e) {
			e.printStackTrace();
			messengerError = e.getMessage();
			return false;
		}
	}
	
	private IProbeMessage exchangeMessage(IProbeMessage message) {
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
							logger.error("Failed to send message: " + messengerError);
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
	
	private void waitForEmptySendQueue() {
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

	private void refreshConfiguration() {
		Thread t = new Thread(new Runnable() {
			public void run() {
				waitForEmptySendQueue();
				final GetSpiderConfiguration response = (GetSpiderConfiguration) exchangeMessage(new GetSpiderConfiguration());
				if(response == null) {
					logger.warning("Failed to get spider configuration: " + getLastError());
					return;
				}
				synchronized(lock) {
					configuration = response.getConfiguration();
					lock.notifyAll();
				}
			}
		});
		t.start();
	}

	private void refreshAvailableModules() {
		Thread t = new Thread(new Runnable() {
			public void run() {
				waitForEmptySendQueue();
				final GetAvailableModules response = (GetAvailableModules) exchangeMessage(new GetAvailableModules());
				if(response == null) {
					logger.warning("Failed to get available modules: " + getLastError());
					return;
				}
				synchronized(lock) {
					availableModules = response.getModules();
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
				GetSpiderStatus status = (GetSpiderStatus) exchangeMessage(new GetSpiderStatus());
				if(status == null) {
					logger.warning("Failed to receive status message");
					return;
				}
				if(status.isRunning != isRunning) {
					isRunning = status.isRunning;
					stateChangeListeners.fireEvent(new IEvent() {});
				}
			}
		});
		t.start();
	}
}
