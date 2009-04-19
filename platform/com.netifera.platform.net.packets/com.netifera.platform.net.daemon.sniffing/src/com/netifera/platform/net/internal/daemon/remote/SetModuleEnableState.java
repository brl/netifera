package com.netifera.platform.net.internal.daemon.remote;

import java.util.ArrayList;
import java.util.List;

import com.netifera.platform.api.dispatcher.ProbeMessage;

public class SetModuleEnableState extends ProbeMessage {
	
	private static final long serialVersionUID = 5333849866496635291L;

	public final static String ID = "SetModuleEnableState";
	
	private final List<ModuleRecord> modules;
	public SetModuleEnableState(String prefix, List<ModuleRecord> modules) {
		super(prefix + ID);
		this.modules = modules;
	}
	
	public SetModuleEnableState(String prefix, ModuleRecord module) {
		super(prefix + ID);
		this.modules = new ArrayList<ModuleRecord>();
		this.modules.add(module);
	}
	
	public List<ModuleRecord> getModuleRecords() {
		return modules;
	}


}
