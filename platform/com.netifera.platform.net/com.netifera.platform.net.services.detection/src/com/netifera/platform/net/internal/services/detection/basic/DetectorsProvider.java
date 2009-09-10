package com.netifera.platform.net.internal.services.detection.basic;

import java.io.InputStream;
import java.util.List;

import org.osgi.service.component.ComponentContext;

import com.netifera.platform.net.services.detection.INetworkServiceDetector;
import com.netifera.platform.net.services.detection.INetworkServiceDetectorProvider;
import com.netifera.platform.net.services.detection.INetworkServiceTrigger;
import com.netifera.platform.net.services.detection.PatternsLoaderXML;
import com.netifera.platform.net.services.detection.TriggersLoaderXML;

public class DetectorsProvider implements INetworkServiceDetectorProvider {
	private List<INetworkServiceDetector> clientDetectors;
	private List<INetworkServiceDetector> serverDetectors;
	private List<INetworkServiceTrigger> serviceTriggers;

	protected void activate(ComponentContext context) {
		InputStream stream;
		System.out.println("********** activating...");
		try {
			stream = context.getBundleContext().getBundle().getEntry("ServerPatterns.xml").openStream();
			PatternsLoaderXML reader = new PatternsLoaderXML(stream);
			serverDetectors = reader.getDetectors();
			System.out.println("************ server detectors count: "+serverDetectors.size());
			stream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			stream = context.getBundleContext().getBundle().getEntry("ClientPatterns.xml").openStream();
			PatternsLoaderXML reader = new PatternsLoaderXML(stream);
			clientDetectors = reader.getDetectors();
			System.out.println("************ client detectors count: "+clientDetectors.size());
			stream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			stream = context.getBundleContext().getBundle().getEntry("Triggers.xml").openStream();
			TriggersLoaderXML  reader = new TriggersLoaderXML(stream);
			serviceTriggers = reader.getTriggers();
			System.out.println("************ service triggers count: "+ serviceTriggers.size());
			stream.close();
		}	catch(Exception e) {
			e.printStackTrace();
		}

	}

	protected void deactivate(ComponentContext context) {
	}

	public List<INetworkServiceDetector> getClientDetectors() {
		return clientDetectors;
	}

	public List<INetworkServiceDetector> getServerDetectors() {
		return serverDetectors;
	}

	public List<INetworkServiceTrigger> getTriggers() {
		return serviceTriggers;
	}
}