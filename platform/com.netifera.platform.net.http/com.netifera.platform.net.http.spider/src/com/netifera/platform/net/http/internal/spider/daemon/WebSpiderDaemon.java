package com.netifera.platform.net.http.internal.spider.daemon;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.osgi.service.component.ComponentContext;

import com.netifera.platform.api.dispatcher.IMessageDispatcherService;
import com.netifera.platform.api.dispatcher.IMessenger;
import com.netifera.platform.api.dispatcher.MessengerException;
import com.netifera.platform.api.log.ILogManager;
import com.netifera.platform.api.log.ILogger;
import com.netifera.platform.net.dns.service.nameresolver.INameResolver;
import com.netifera.platform.net.http.internal.spider.daemon.remote.FetchURL;
import com.netifera.platform.net.http.internal.spider.daemon.remote.GetAvailableModules;
import com.netifera.platform.net.http.internal.spider.daemon.remote.GetSpiderConfiguration;
import com.netifera.platform.net.http.internal.spider.daemon.remote.GetSpiderStatus;
import com.netifera.platform.net.http.internal.spider.daemon.remote.SetSpiderConfiguration;
import com.netifera.platform.net.http.internal.spider.daemon.remote.StartSpider;
import com.netifera.platform.net.http.internal.spider.daemon.remote.StopSpider;
import com.netifera.platform.net.http.internal.spider.daemon.remote.VisitURL;
import com.netifera.platform.net.http.internal.spider.daemon.remote.WebSpiderConfiguration;
import com.netifera.platform.net.http.spider.IWebSpiderModule;
import com.netifera.platform.net.http.spider.impl.WebSpider;
import com.netifera.platform.net.http.web.model.IWebEntityFactory;

public class WebSpiderDaemon implements IWebSpiderMessageHandler {

	/* subsystems */
	private final WebSpiderMessageDispatcher messages;

	/* OSGi Services */
	private ILogger logger;
//	private IProbeManagerService probeManager;
	private IMessageDispatcherService dispatcher;
	private IWebEntityFactory factory;
	private INameResolver resolver;

	/* spider */
	private final WebSpider spider;
	private final List<IWebSpiderModule> modules;

	private Thread spiderThread;

	
	protected WebSpiderDaemon() {
		spider = new WebSpider();
		messages = new WebSpiderMessageDispatcher(this);
		modules = new ArrayList<IWebSpiderModule>();
	}

	/*
	 * OSGi DS binding
	 */

	protected void activate(ComponentContext ctx) {
		spider.setServices(logger, factory, resolver);
		messages.setServices(logger, dispatcher.getServerDispatcher());
		messages.registerHandlers();
	}

	protected void deactivate(ComponentContext ctx) { }

	protected void registerModule(IWebSpiderModule module) {
		modules.add(module);
	}

	protected void unregisterModule(IWebSpiderModule module) {
		modules.remove(module);
	}

/*	protected void setProbeManager(IProbeManagerService manager) {
		this.probeManager = manager;
	}

	protected void unsetProbeManager(IProbeManagerService manager) {}
*/
	protected void setDispatcher(IMessageDispatcherService dispatcher) {
		this.dispatcher = dispatcher;
	}

	protected void unsetDispatcher(IMessageDispatcherService dispatcher) {}

	protected void setLogManager(ILogManager logManager) {
		logger = logManager.getLogger("Web Spider Daemon");
	}

	protected void unsetLogManager(ILogManager logManager) {}

	protected void setFactory(IWebEntityFactory factory) {
		this.factory = factory;
	}

	protected void unsetFactory(IWebEntityFactory factory) {
		this.factory = null;
	}

	protected void setResolver(INameResolver resolver) {
		this.resolver = resolver;
	}

	protected void unsetResolver(INameResolver resolver) {
		this.resolver = null;
	}

	
	/* Message Handlers */

	public void getAvailableModules(IMessenger messenger, GetAvailableModules msg) throws MessengerException {
		Set<String> moduleNames = new HashSet<String>();
		for (IWebSpiderModule module: modules)
			moduleNames.add(module.getName());
		messenger.emitMessage(msg.createResponse(moduleNames));
	}

	public void getSpiderConfiguration(IMessenger messenger, GetSpiderConfiguration msg) throws MessengerException {
		WebSpiderConfiguration config = new WebSpiderConfiguration();
		config.bufferSize = spider.getBufferSize();
		config.queueSize = spider.getQueueSize();
		config.maximumConnections = spider.getMaximumConnections();
		config.fetchImages = spider.getFetchImages();
		config.followLinks = spider.getFollowLinks();
		config.modules = new HashSet<String>();
		for (IWebSpiderModule module: spider.getModules())
			config.modules.add(module.getName());
		messenger.emitMessage(msg.createResponse(config));
	}

	public void setSpiderConfiguration(IMessenger messenger, SetSpiderConfiguration msg) throws MessengerException {
		WebSpiderConfiguration config = msg.getConfiguration();
		spider.setBufferSize(config.bufferSize);
		spider.setQueueSize(config.queueSize);
		spider.setMaximumConnections(config.maximumConnections);
		spider.setFetchImages(config.fetchImages);
		spider.setFollowLinks(config.followLinks);
		List<IWebSpiderModule> modulesToRemove = new ArrayList<IWebSpiderModule>();
		for (IWebSpiderModule module: spider.getModules())
			if (! config.modules.contains(module.getName()))
				modulesToRemove.add(module);
		for (IWebSpiderModule module: modulesToRemove)
			spider.removeModule(module);
		for (String moduleName: config.modules)
			for (IWebSpiderModule module: modules)
				if (module.getName().equals(moduleName)) {
					spider.addModule(module);
					break;
				}
	}

	public void getSpiderStatus(IMessenger messenger, GetSpiderStatus msg) throws MessengerException {
		messenger.emitMessage(msg.createResponse(isRunning()));
	}

	public void startSpider(IMessenger messenger, StartSpider msg) throws MessengerException {
		if(isRunning()) {
			messenger.respondError(msg, "Web Spider already running");
			return;
		}
		spider.setSpaceId(msg.getSpaceId());
		spiderThread = new Thread(new Runnable() {
			public void run() {
				try {
					spider.run();
				} catch (InterruptedException e) {
					logger.debug("Spider interrupted", e);
				} catch (IOException e) {
					logger.error("Spider error", e);
				}
			}
		});
		spiderThread.start();
	}

	public void stopSpider(IMessenger messenger, StopSpider msg) throws MessengerException {
		if (!isRunning()) {
			messenger.respondError(msg, "Web Spider is not running");
			return;
		}
		spiderThread.interrupt();
		spiderThread = null;
	}

	public void visitURL(IMessenger messenger, VisitURL msg) throws MessengerException {
		spider.visit(msg.url);
	}

	public void fetchURL(IMessenger messenger, FetchURL msg) throws MessengerException {
		spider.fetch(msg.url, msg.method, msg.headers, msg.content);
	}

	private boolean isRunning() {
		return spiderThread != null && spiderThread.isAlive();
	}
}
