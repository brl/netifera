package com.netifera.platform.net.internal.daemon.sniffing;

import com.netifera.platform.api.events.IEventHandler;
import com.netifera.platform.api.probe.IProbe;
import com.netifera.platform.net.daemon.sniffing.extend.AbstractSniffingDaemonFactory;
import com.netifera.platform.net.daemon.sniffing.extend.AbstractSniffingDaemonStub;

public class SniffingDaemonFactory extends AbstractSniffingDaemonFactory {

	
	public SniffingDaemonFactory() {
		super(SniffingDaemon.MESSAGE_PREFIX);
	}

	@Override
	protected AbstractSniffingDaemonStub createStub(IProbe probe, IEventHandler changeHandler) {
		return new SniffingDaemonStub(probe, getLogger(), changeHandler);
	}

}
