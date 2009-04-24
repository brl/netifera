package com.netifera.platform.net.http.spider.modules;

import com.netifera.platform.net.http.spider.HTTPRequest;
import com.netifera.platform.net.http.spider.HTTPResponse;
import com.netifera.platform.net.http.spider.IWebSpiderContext;
import com.netifera.platform.net.http.spider.IWebSpiderModule;
import com.netifera.platform.net.http.web.model.IWebEntityFactory;

public class HTTPBasicAuthExtractorModule implements IWebSpiderModule {

	private IWebEntityFactory factory;
	
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
					context.getLogger().info("Basic authentication realm \""+authRealm+"\" at "+request.getURL());
					factory.createBasicAuthentication(context.getRealm(), context.getSpaceId(), context.getSocketLocator(), request.getURL(), authRealm);
				}
			}
		}
	}

	public void start(IWebSpiderContext context) {
	}

	public void stop() {
	}
	
	protected void setFactory(IWebEntityFactory factory) {
		this.factory = factory;
	}
	
	protected void unsetFactory(IWebEntityFactory factory) {
		this.factory = null;
	}
}
