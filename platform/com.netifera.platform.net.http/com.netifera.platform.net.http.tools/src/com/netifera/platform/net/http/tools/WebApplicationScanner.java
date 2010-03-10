package com.netifera.platform.net.http.tools;

import java.io.IOException;

import com.netifera.platform.api.tools.ITool;
import com.netifera.platform.api.tools.IToolContext;
import com.netifera.platform.api.tools.ToolException;
import com.netifera.platform.net.http.internal.tools.Activator;
import com.netifera.platform.net.http.service.HTTP;
import com.netifera.platform.net.http.spider.impl.WebSpider;
import com.netifera.platform.net.http.spider.modules.WebApplicationDetectorModule;

public class WebApplicationScanner implements ITool {
	private IToolContext context;
	private HTTP http;
	private String hostname;

	public void run(IToolContext context) throws ToolException {
		this.context = context;

		context.setTitle("Web applications scanner");
		setupToolOptions();

		context.setTitle("Scan web applications at "+http.getSocketAddress());
		try {
			WebSpider spider = new WebSpider();
			spider.setServices(context.getLogger(), Activator.getInstance().getWebEntityFactory(), Activator.getInstance().getNameResolver());
			spider.setRealm(context.getRealm());
			spider.setSpaceId(context.getSpaceId());
			spider.addTarget(http, hostname);
			spider.setFollowLinks(false);
			if (context.getConfiguration().get("maximumConnections") != null)
				spider.setMaximumConnections((Integer)context.getConfiguration().get("maximumConnections"));
			if (context.getConfiguration().get("bufferSize") != null)
				spider.setBufferSize((Integer)context.getConfiguration().get("bufferSize"));
			
			spider.addModule(WebApplicationDetectorModule.class.getName());
			spider.run();
		} catch (IOException e) {
			context.exception("I/O error: " + e.getMessage(), e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			context.warning("Interrupted");
		} finally {
			context.done();
		}
	}
	
	private void setupToolOptions() {
		http = (HTTP) context.getConfiguration().get("target");
		hostname = (String) context.getConfiguration().get("hostname");
		if (hostname == null) {
			hostname = http.getURIHost();
		}
	}
}
