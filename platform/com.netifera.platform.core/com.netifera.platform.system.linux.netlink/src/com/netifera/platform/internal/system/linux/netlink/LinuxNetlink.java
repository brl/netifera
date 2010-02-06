package com.netifera.platform.internal.system.linux.netlink;

import com.netifera.platform.api.log.ILogManager;
import com.netifera.platform.api.log.ILogger;
import com.netifera.platform.api.system.ISystemService;
import com.netifera.platform.system.linux.netlink.ILinuxNetlink;

public class LinuxNetlink implements ILinuxNetlink {
	
	private ILogger logger;
	private ISystemService system;

	
	
	protected void setLogManager(ILogManager logManager) {
		this.logger = logManager.getLogger("Linux Netlink");
	}

	protected void unsetLogManager(ILogManager logManager) {}

	protected void setSystemService(ISystemService system) {
		this.system = system;
	}

	protected void unsetSystemService(ISystemService system) {}

}
