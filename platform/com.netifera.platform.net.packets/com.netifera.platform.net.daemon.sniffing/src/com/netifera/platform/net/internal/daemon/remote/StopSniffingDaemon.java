package com.netifera.platform.net.internal.daemon.remote;

import com.netifera.platform.api.dispatcher.ProbeMessage;

public class StopSniffingDaemon extends ProbeMessage {
	
	private static final long serialVersionUID = -6156273771742548467L;
	public final static String ID = "StopSniffingDaemon";
	
	public StopSniffingDaemon(String prefix) {
		super(prefix + ID);
	}
}
