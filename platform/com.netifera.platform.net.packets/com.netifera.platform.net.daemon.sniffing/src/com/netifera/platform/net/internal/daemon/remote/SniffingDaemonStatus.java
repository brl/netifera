package com.netifera.platform.net.internal.daemon.remote;

import com.netifera.platform.api.dispatcher.ProbeMessage;

public class SniffingDaemonStatus extends ProbeMessage {

	private static final long serialVersionUID = -5885512219887463301L;

	public final static String ID = "SniffingDaemonStatus";

	private final boolean isRunning;
	public SniffingDaemonStatus(String prefix) {
		super(prefix + ID);
		isRunning = false;
	}
	
	public boolean isRunning() {
		return isRunning;
	}
	public SniffingDaemonStatus createResponse(String prefix, boolean isRunning) {
		return new SniffingDaemonStatus(prefix, isRunning, getSequenceNumber());
	}
	private SniffingDaemonStatus(String prefix, boolean isRunning, int sequenceNumber) {
		super(prefix + ID);
		this.isRunning = isRunning;
		setSequenceNumber(sequenceNumber);
		markAsResponse();
	}

}
