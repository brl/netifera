package com.netifera.platform.net.http.spider;

import java.net.URI;


public interface HTTPRequest {
	String getMethod();
	URI getURL();
	String getHeader(String headerName);
	String toString();
}
