package com.netifera.platform.net.http.spider;

public interface IWebSpiderModule {
	String getName();

	void start(IWebSpiderContext context);
	void handle(IWebSpiderContext context, HTTPRequest request, HTTPResponse response);
	void stop();
}
