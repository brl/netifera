package com.netifera.platform.net.http.spider.modules;

import java.net.URI;

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
				URI base = context.getBaseURL().resolve(request.getURL());
				context.getSpider().visit(base.resolve("web.config"));
				context.getSpider().visit(base.resolve("machine.config"));
				context.getSpider().visit(base.resolve("default.asp"));
				context.getSpider().visit(base.resolve("default.aspx"));
			}
		}
	}

	public void start(IWebSpiderContext context) {
	}

	public void stop() {
	}
}
