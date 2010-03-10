package com.netifera.platform.net.http.internal.spider.daemon.remote;

import com.netifera.platform.api.dispatcher.ProbeMessage;

public class GetSpiderStatus extends ProbeMessage {

	private static final long serialVersionUID = -456297618103640487L;

	public final static String ID = "GetSpiderStatus";

	final boolean isRunning;
	
	public GetSpiderStatus() {
		super(ID);
		isRunning = false;
	}
	
	public boolean isRunning() {
		return isRunning;
	}
	
	public GetSpiderStatus createResponse(boolean isRunning) {
		return new GetSpiderStatus(isRunning, getSequenceNumber());
	}
	
	private GetSpiderStatus(boolean isRunning, int sequenceNumber) {
		super(ID);
		this.isRunning = isRunning;
		setSequenceNumber(sequenceNumber);
		markAsResponse();
	}
}
