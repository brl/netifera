package com.netifera.platform.net.http.spider.modules;

import java.util.Map;

import com.netifera.platform.net.http.internal.tools.Activator;
import com.netifera.platform.net.http.spider.HTTPRequest;
import com.netifera.platform.net.http.spider.HTTPResponse;
import com.netifera.platform.net.http.spider.IWebSpiderContext;
import com.netifera.platform.net.http.spider.IWebSpiderModule;


public class WebApplicationDetectorModule implements IWebSpiderModule {

	public String getName() {
		return "Detect Web Applications";
	}

	public void start(IWebSpiderContext context) {
		// send requests for web apps default urls
		for (String url: Activator.getInstance().getWebApplicationDetector().getTriggers())
			context.getSpider().get(context.getBaseURL().resolve(url));
	}
	
	public void stop() {
	}
	
	public void handle(IWebSpiderContext context, HTTPRequest request, HTTPResponse response) {
		Map<String,String> serviceInfo = Activator.getInstance().getWebApplicationDetector().detect(request.toString(), response.toString());
		if (serviceInfo != null) {
			Activator.getInstance().getWebEntityFactory().createWebApplication(context.getRealm(), context.getSpaceId(), context.getLocator(), request.getURL(), serviceInfo);
			context.getToolContext().info(serviceInfo.get("serviceType")+" detected at "+request.getURL());
		}
	}
}
