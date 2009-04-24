package com.netifera.platform.net.http.spider.modules;

import com.netifera.platform.net.http.spider.HTTPRequest;
import com.netifera.platform.net.http.spider.HTTPResponse;
import com.netifera.platform.net.http.spider.IWebSpiderContext;
import com.netifera.platform.net.http.spider.IWebSpiderModule;

public class CrawlBackupFilesModule implements IWebSpiderModule {

	public String getName() {
		return "Crawl Backup Files";
	}

	public void handle(IWebSpiderContext context, HTTPRequest request, HTTPResponse response) {
		if (response.getContentType() != null) {
			if (response.getStatusCode() == 200) {
				if (request.getURL().getPath().toLowerCase().matches(".*\\.(php|asp|aspx|jsp)$")) {
					context.getSpider().visit(request.getURL().resolve(request.getURL().getPath()+"~"));
					context.getSpider().visit(request.getURL().resolve(request.getURL().getPath()+".bak"));
					context.getSpider().visit(request.getURL().resolve(request.getURL().getPath().replaceAll("\\.[^.]+$", ".bak")));
					context.getSpider().visit(request.getURL().resolve(request.getURL().getPath()+".old"));
					context.getSpider().visit(request.getURL().resolve(request.getURL().getPath().replaceAll("\\.[^.]+$", ".old")));
				}
			}
		}
	}

	public void start(IWebSpiderContext context) {
	}

	public void stop() {
	}
}
