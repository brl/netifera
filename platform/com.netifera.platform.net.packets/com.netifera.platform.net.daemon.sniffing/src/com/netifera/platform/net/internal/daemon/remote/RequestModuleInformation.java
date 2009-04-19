package com.netifera.platform.net.internal.daemon.remote;

import java.util.List;

import com.netifera.platform.api.dispatcher.ProbeMessage;

public class RequestModuleInformation extends ProbeMessage {
	
	private static final long serialVersionUID = 2551440464627254907L;

	public final static String ID = "RequestModuleInformation";

	private final List<ModuleRecord> modules;
	
	public RequestModuleInformation(String prefix) {
		super(prefix + ID);
		modules = null;
	}
	
	public RequestModuleInformation createResponse(String prefix, List<ModuleRecord> modules) {
		return new RequestModuleInformation(prefix, modules, getSequenceNumber());
	}
	
	private RequestModuleInformation(String prefix, List<ModuleRecord> modules, int sequenceNumber) {
		super(prefix + ID);
		this.modules = modules;
		setSequenceNumber(sequenceNumber);
		markAsResponse();
	}
	
	public List<ModuleRecord> getModuleRecords() {
		return modules;
	}
}

