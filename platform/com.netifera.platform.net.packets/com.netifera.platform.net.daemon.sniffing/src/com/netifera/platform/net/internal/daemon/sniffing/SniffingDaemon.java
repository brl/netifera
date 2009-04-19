package com.netifera.platform.net.internal.daemon.sniffing;

import java.util.Arrays;
import java.util.Collection;

import com.netifera.platform.net.daemon.sniffing.extend.AbstractSniffingDaemon;
import com.netifera.platform.net.daemon.sniffing.model.SniffingSessionEntity;
import com.netifera.platform.net.pcap.ICaptureInterface;
import com.netifera.platform.net.sniffing.ICaptureFileInterface;
import com.netifera.platform.net.sniffing.ISniffingEngineService;

public class SniffingDaemon extends AbstractSniffingDaemon {
	public final static String MESSAGE_PREFIX = "sniffer";

	private ISniffingEngineService sniffingEngine;

	public SniffingDaemon() {
		super(MESSAGE_PREFIX);
	}
	
	protected void setSniffingEngine(ISniffingEngineService sniffingEngine) {
		this.sniffingEngine = sniffingEngine;
	}
	
	protected void unsetSniffingEngine(ISniffingEngineService sniffingEngine) {
		
	}

	@Override
	public ISniffingEngineService getSniffingEngine() {
		return sniffingEngine;
	}
	
	public void doStart(long spaceId, Collection<ICaptureInterface> enabledInterfaces) {
		final long realmId = getProbeManager().getLocalProbe().getEntity().getId();
		final SniffingSessionEntity sessionEntity = getSniffingEntityFactory().createSniffingSession(realmId, spaceId);
		getModuleExecutionProvider().startModules(enabledInterfaces, spaceId, sessionEntity.getId(), true);
	}
	
	public void doStartCaptureFile(long spaceId, long realmId, ICaptureFileInterface captureFileInterface) {
		getModuleExecutionProvider().startModules(Arrays.asList((ICaptureInterface)captureFileInterface), spaceId, realmId, false);
	}
	
	public void doStop() {
		getModuleExecutionProvider().stopModules();
	}

	
}