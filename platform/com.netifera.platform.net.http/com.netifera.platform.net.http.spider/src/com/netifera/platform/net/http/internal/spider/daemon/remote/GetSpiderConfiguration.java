package com.netifera.platform.net.http.internal.spider.daemon.remote;

import com.netifera.platform.api.dispatcher.ProbeMessage;

public class GetSpiderConfiguration extends ProbeMessage {
	
	private static final long serialVersionUID = 554968420035522058L;

	public final static String ID = "GetSpiderConfiguration";

	private final WebSpiderConfiguration configuration;
	
	public GetSpiderConfiguration() {
		super(ID);
		configuration = null;
	}
	
	public GetSpiderConfiguration createResponse(WebSpiderConfiguration configuration) {
		return new GetSpiderConfiguration(configuration, getSequenceNumber());
	}
	
	private GetSpiderConfiguration(WebSpiderConfiguration configuration, int sequenceNumber) {
		super(ID);
		this.configuration = configuration;
		setSequenceNumber(sequenceNumber);
		markAsResponse();
	}
	
	public WebSpiderConfiguration getConfiguration() {
		return configuration;
	}
}

