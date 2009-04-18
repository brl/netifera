package com.netifera.platform.net.http.spider.impl;

import java.net.URI;

import org.apache.http.Header;
import org.apache.http.HttpRequest;

import com.netifera.platform.net.http.spider.HTTPRequest;

public class HTTPRequestAdapter implements HTTPRequest {

	private final HttpRequest request;
	
	public HTTPRequestAdapter(HttpRequest request) {
		this.request = request;
	}
	
	public String getHeader(String headerName) {
		Header header = request.getFirstHeader(headerName);
		return header == null ? null : header.getValue();
	}

	public String getMethod() {
		return request.getRequestLine().getMethod();
	}

	public URI getURL() {
		return URI.create(request.getRequestLine().getUri());
	}
	
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append(request.getRequestLine());
		buffer.append('\n');
		for (Header header: request.getAllHeaders()) {
			buffer.append(header);
			buffer.append('\n');
		}
		
		return buffer.toString();
	}
}
