package com.netifera.platform.net.http.internal.spider.daemon.remote;

import com.netifera.platform.api.dispatcher.ProbeMessage;

public class StartSpider extends ProbeMessage {
	
	private static final long serialVersionUID = -3899000786905111812L;

	public final static String ID = "StartSpider";
	
	private final long spaceId;
	
	public StartSpider(long spaceId) {
		super(ID);
		this.spaceId = spaceId;
	}
	
	public long getSpaceId() {
		return spaceId;
	}
}
