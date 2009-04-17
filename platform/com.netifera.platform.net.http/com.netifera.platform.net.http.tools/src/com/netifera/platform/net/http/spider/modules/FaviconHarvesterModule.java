package com.netifera.platform.net.http.spider.modules;

import java.io.IOException;

import com.netifera.platform.net.http.internal.tools.Activator;
import com.netifera.platform.net.http.spider.HTTPRequest;
import com.netifera.platform.net.http.spider.HTTPResponse;
import com.netifera.platform.net.http.spider.IWebSpiderContext;
import com.netifera.platform.net.http.spider.IWebSpiderModule;

public class FaviconHarvesterModule implements IWebSpiderModule {

	public String getName() {
		return "Extract Favicon";
	}

	public void handle(IWebSpiderContext context, HTTPRequest request, HTTPResponse response) {
		if (response.getContentType() != null) {
			String contentType = response.getContentType();
			
			// is favicon? get it and add it to the model
			if (response.getStatusCode() == 200 && request.getURL().getPath().equals("/favicon.ico") && contentType.matches("image/x-icon|application/octet-stream|text/plain")) {
//				entity.consumeContent();
				int length = (int) response.getContentLength();
				if (length > 0) {
					byte[] content;
					try {
						content = response.getContent();
						Activator.getInstance().getWebEntityFactory().setFavicon(context.getRealm(), context.getSpaceId(), context.getLocator(), context.getBaseURL().resolve(request.getURL()), content);
					} catch (IOException e) {
						context.getToolContext().exception("Exception in Favicon module", e);
						e.printStackTrace();
					}
				}
			}
		}
	}

	public void start(IWebSpiderContext context) {
		context.getSpider().get(context.getBaseURL().resolve("/favicon.ico"));
	}

	public void stop() {
	}
}
