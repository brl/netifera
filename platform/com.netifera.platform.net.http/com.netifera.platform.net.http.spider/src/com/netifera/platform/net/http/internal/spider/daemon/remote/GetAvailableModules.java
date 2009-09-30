package com.netifera.platform.net.http.internal.spider.daemon.remote;

import java.util.Set;

import com.netifera.platform.api.dispatcher.ProbeMessage;

public class GetAvailableModules extends ProbeMessage {
	
	private static final long serialVersionUID = -7604877647651415508L;

	public final static String ID = "GetAvailableModules";

	private final Set<String> modules;
	
	public GetAvailableModules() {
		super(ID);
		modules = null;
	}
	
	public GetAvailableModules createResponse(Set<String> modules) {
		return new GetAvailableModules(modules, getSequenceNumber());
	}
	
	private GetAvailableModules(Set<String> modules, int sequenceNumber) {
		super(ID);
		this.modules = modules;
		setSequenceNumber(sequenceNumber);
		markAsResponse();
	}
	
	public Set<String> getModules() {
		return modules;
	}
}
