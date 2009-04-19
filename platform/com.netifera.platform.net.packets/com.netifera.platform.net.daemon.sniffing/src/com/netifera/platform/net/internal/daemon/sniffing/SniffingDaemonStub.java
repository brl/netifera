package com.netifera.platform.net.internal.daemon.sniffing;

import com.netifera.platform.api.events.IEventHandler;
import com.netifera.platform.api.log.ILogger;
import com.netifera.platform.api.probe.IProbe;
import com.netifera.platform.net.daemon.sniffing.extend.AbstractSniffingDaemonStub;

public class SniffingDaemonStub extends AbstractSniffingDaemonStub {
	
	public SniffingDaemonStub(IProbe probe, ILogger logger, IEventHandler changeHandler) {
		super(SniffingDaemon.MESSAGE_PREFIX, probe, logger, changeHandler);
	}

}
