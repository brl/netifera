package com.netifera.platform.net.http.internal.spider.daemon.remote;

import com.netifera.platform.api.dispatcher.ProbeMessage;

public class SetSpiderConfiguration extends ProbeMessage {
	
	private static final long serialVersionUID = 851420142810909653L;

	public final static String ID = "SetSpiderConfiguration";

	private final WebSpiderConfiguration configuration;
	
	
	public SetSpiderConfiguration(WebSpiderConfiguration configuration) {
		super(ID);
		this.configuration = configuration;
	}
	
	public WebSpiderConfiguration getConfiguration() {
		return configuration;
	}
}

