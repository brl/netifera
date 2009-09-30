package com.netifera.platform.net.http.spider;

import java.net.URI;
import java.util.Map;


public interface IWebSpider {
	void fetch(URI url, String method, Map<String,String> headers, String content) throws OutOfScopeException;
	void visit(URI url) throws OutOfScopeException;
}
