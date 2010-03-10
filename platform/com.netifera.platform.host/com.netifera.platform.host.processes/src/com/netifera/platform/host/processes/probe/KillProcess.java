package com.netifera.platform.host.processes.probe;

import com.netifera.platform.api.dispatcher.ProbeMessage;

public class KillProcess extends ProbeMessage {
	
	private static final long serialVersionUID = -8855470532062989219L;

	public static final String ID = "KillProcess";

	private final Integer pid;
	private final Boolean result;
	
	KillProcess(int pid) {
		super(ID);
		this.pid = pid;
		this.result = null;
	}
	
	KillProcess createResponse(boolean result) {
		return new KillProcess(result, getSequenceNumber());
	}
	
	private KillProcess(boolean result, int sequenceNumber) {
		super(ID);
		this.pid = null;
		this.result = result;
		setSequenceNumber(sequenceNumber);
		markAsResponse();
	}

	int getPID() {
		return pid;
	}
	
	boolean getResult() {
		return result;
	}
}
