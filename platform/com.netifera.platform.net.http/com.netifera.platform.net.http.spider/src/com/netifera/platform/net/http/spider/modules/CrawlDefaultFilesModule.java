package com.netifera.platform.net.http.spider.modules;

import com.netifera.platform.net.http.spider.HTTPRequest;
import com.netifera.platform.net.http.spider.HTTPResponse;
import com.netifera.platform.net.http.spider.IWebSpiderContext;
import com.netifera.platform.net.http.spider.IWebSpiderModule;

public class CrawlDefaultFilesModule implements IWebSpiderModule {

	public String getName() {
		return "Crawl Default And Config Files";
	}

	public void handle(IWebSpiderContext context, HTTPRequest request, HTTPResponse response) {
		if (response.getContentType() != null) {
			if (response.getStatusCode() == 200) {
				context.getSpider().get(request.getURL().resolve("web.config"));
				context.getSpider().get(request.getURL().resolve("machine.config"));
				context.getSpider().get(request.getURL().resolve("default.asp"));
				context.getSpider().get(request.getURL().resolve("default.aspx"));
			}
		}
	}

	public void start(IWebSpiderContext context) {
	}

	public void stop() {
	}
}
