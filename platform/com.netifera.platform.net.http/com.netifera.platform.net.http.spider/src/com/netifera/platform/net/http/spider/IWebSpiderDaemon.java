package com.netifera.platform.net.http.spider;

import java.util.Set;

import com.netifera.platform.api.daemon.IDaemon;

public interface IWebSpiderDaemon extends IDaemon, IWebSpider {
	Set<IWebSpiderModule> getModules();
	boolean isEnabled(IWebSpiderModule module);
	void setEnabled(IWebSpiderModule module, boolean enable);
	void enableModules(Set<IWebSpiderModule> enabledModuleSet);
}