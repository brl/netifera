package com.netifera.platform.net.http.spider.daemon;

import com.netifera.platform.api.events.IEventHandler;
import com.netifera.platform.api.probe.IProbe;

public interface IWebSpiderDaemonFactory {
	IWebSpiderDaemon createForProbe(IProbe probe, IEventHandler changeHandler);
	IWebSpiderDaemon lookupForProbe(IProbe probe);
}
