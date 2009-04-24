package com.netifera.platform.net.http.internal.spider.daemon.remote;

import java.net.URI;
import java.util.Map;

import com.netifera.platform.api.dispatcher.ProbeMessage;

public class FetchURL extends ProbeMessage {
	
	private static final long serialVersionUID = -8782916028612047410L;

	public final static String ID = "FetchURL";

	final public URI url;
	final public String method;
	final public Map<String,String> headers;
	final public String content;	
	
	public FetchURL(URI url, String method, Map<String,String> headers, String content) {
		super(ID);
		this.url = url;
		this.method = method;
		this.headers = headers;
		this.content = content;
	}
}

