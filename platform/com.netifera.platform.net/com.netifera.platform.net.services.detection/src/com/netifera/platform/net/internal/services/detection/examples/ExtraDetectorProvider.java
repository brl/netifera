package com.netifera.platform.net.internal.services.detection.examples;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.netifera.platform.net.services.detection.INetworkServiceDetector;
import com.netifera.platform.net.services.detection.INetworkServiceDetectorProvider;
import com.netifera.platform.net.services.detection.INetworkServiceTrigger;

public class ExtraDetectorProvider implements INetworkServiceDetectorProvider {

	public List<INetworkServiceDetector> getClientDetectors() {
		return Collections.emptyList();
	}

	public List<INetworkServiceDetector> getServerDetectors() {
		List<INetworkServiceDetector> detectors = new ArrayList<INetworkServiceDetector>();
		detectors.add(new MSSQLDetector());
		detectors.add(new OracleDetector());
		return detectors;
	}

	public List<INetworkServiceTrigger> getTriggers() {
		return Collections.emptyList();
	}
}