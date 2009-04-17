package com.netifera.platform.net.http.spider.modules;

import com.netifera.platform.net.http.internal.tools.Activator;
import com.netifera.platform.net.http.spider.HTTPRequest;
import com.netifera.platform.net.http.spider.HTTPResponse;
import com.netifera.platform.net.http.spider.IWebSpiderContext;
import com.netifera.platform.net.http.spider.IWebSpiderModule;

public class HTTPBasicAuthExtractorModule implements IWebSpiderModule {

	public String getName() {
		return "Extract HTTP Basic Authentication";
	}

	public void handle(IWebSpiderContext context, HTTPRequest request, HTTPResponse response) {
		if (response.getStatusCode() == 401) {
			String header = response.getHeader("WWW-Authenticate");
			if (header != null) {
				String method = header.split(" ")[0];
				if (method.toLowerCase().equals("basic")) {
					String authRealm = header.split("\"")[1];
					context.getToolContext().info("Basic authentication realm \""+authRealm+"\" at "+request.getURL());
					Activator.getInstance().getWebEntityFactory().createBasicAuthentication(context.getRealm(), context.getSpaceId(), context.getLocator(), request.getURL(), authRealm);
				}
			}
		}
	}

	public void start(IWebSpiderContext context) {
	}

	public void stop() {
	}
}
