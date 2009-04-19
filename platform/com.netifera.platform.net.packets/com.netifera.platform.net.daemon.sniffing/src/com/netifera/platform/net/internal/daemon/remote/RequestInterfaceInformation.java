package com.netifera.platform.net.internal.daemon.remote;

import java.util.List;

import com.netifera.platform.api.dispatcher.ProbeMessage;

public class RequestInterfaceInformation extends ProbeMessage {
	
	private static final long serialVersionUID = 6258301182787202475L;

	public final static String ID = "RequestInterfaceInformation";

	private final List<InterfaceRecord> interfaces;
	
	public RequestInterfaceInformation(String prefix) {
		super(prefix + ID);
		interfaces = null;
	}
	
	public RequestInterfaceInformation createResponse(String prefix, List<InterfaceRecord> interfaces) {
		return new RequestInterfaceInformation(prefix, interfaces, getSequenceNumber());
	}
	
	private RequestInterfaceInformation(String prefix, List<InterfaceRecord> interfaces, int sequenceNumber) {
		super(prefix + ID);
		this.interfaces = interfaces;
		setSequenceNumber(sequenceNumber);
		markAsResponse();
	}
	
	public List<InterfaceRecord> getInterfaceRecords() {
		return interfaces;
	}

}
