package com.netifera.platform.net.http.internal.spider.daemon.remote;

import java.net.URI;

import com.netifera.platform.api.dispatcher.ProbeMessage;

public class VisitURL extends ProbeMessage {
	
	private static final long serialVersionUID = 881416997178988221L;

	public final static String ID = "VisitURL";

	final public URI url;
	
	public VisitURL(URI url) {
		super(ID);
		this.url = url;
	}
}

