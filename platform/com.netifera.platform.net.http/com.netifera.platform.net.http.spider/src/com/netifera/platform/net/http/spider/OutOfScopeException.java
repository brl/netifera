package com.netifera.platform.net.http.spider;

import java.net.URI;

public class OutOfScopeException extends RuntimeException {
	private static final long serialVersionUID = 1842739612483327136L;

	private final URI url;

	public OutOfScopeException(URI url) {
		this("Out Of Scope: "+url, url);
	}
	
	public OutOfScopeException(String message, URI url) {
		super(message);
		this.url = url;
	}
	
	public URI getURL() {
		return url;
	}
}
