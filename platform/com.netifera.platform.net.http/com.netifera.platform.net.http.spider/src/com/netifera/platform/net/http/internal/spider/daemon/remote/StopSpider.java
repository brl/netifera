package com.netifera.platform.net.http.internal.spider.daemon.remote;

import com.netifera.platform.api.dispatcher.ProbeMessage;

public class StopSpider extends ProbeMessage {
	
	private static final long serialVersionUID = -4738870595794306558L;
	
	public final static String ID = "StopSpider";
	
	public StopSpider() {
		super(ID);
	}
}
