package com.netifera.platform.net.http.spider.modules;

import java.util.Map;

import com.netifera.platform.net.http.spider.HTTPRequest;
import com.netifera.platform.net.http.spider.HTTPResponse;
import com.netifera.platform.net.http.spider.IWebSpiderContext;
import com.netifera.platform.net.http.spider.IWebSpiderModule;
import com.netifera.platform.net.http.web.applications.IWebApplicationDetector;
import com.netifera.platform.net.http.web.model.IWebEntityFactory;


public class WebApplicationDetectorModule implements IWebSpiderModule {

	private IWebApplicationDetector detector;
	private IWebEntityFactory factory;
	
	public String getName() {
		return "Detect Web Applications";
	}

	public void start(IWebSpiderContext context) {
		// send requests for web apps default urls
		for (String url: detector.getTriggers())
			context.getSpider().visit(context.getBaseURL().resolve(url));
	}
	
	public void stop() {
	}
	
	public void handle(IWebSpiderContext context, HTTPRequest request, HTTPResponse response) {
		Map<String,String> serviceInfo = detector.detect(request.toString(), response.toString());
		if (serviceInfo != null) {
			factory.createWebApplication(context.getRealm(), context.getSpaceId(), context.getSocketLocator(), request.getURL(), serviceInfo);
			context.getLogger().info(serviceInfo.get("serviceType")+" detected at "+request.getURL());
		}
	}
	
	protected void setFactory(IWebEntityFactory factory) {
		this.factory = factory;
	}
	
	protected void unsetFactory(IWebEntityFactory factory) {
		this.factory = null;
	}

	protected void setDetector(IWebApplicationDetector detector) {
		this.detector = detector;
	}

	protected void unsetDetector(IWebApplicationDetector detector) {
		this.detector = null;
	}
}
