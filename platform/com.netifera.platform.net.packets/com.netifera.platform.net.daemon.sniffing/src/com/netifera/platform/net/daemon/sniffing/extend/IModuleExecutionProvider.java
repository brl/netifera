package com.netifera.platform.net.daemon.sniffing.extend;

import java.util.Collection;

import com.netifera.platform.net.pcap.ICaptureInterface;

public interface IModuleExecutionProvider {
	void startModules(Collection<ICaptureInterface> interfaces, long spaceId, long realmId, boolean createInterfaceEntities);
	void stopModules();
}
