package com.netifera.platform.net.internal.services.detection.examples;

import java.io.InputStream;
import java.util.List;

import org.osgi.service.component.ComponentContext;

import com.netifera.platform.net.services.detection.INetworkServiceDetector;
import com.netifera.platform.net.services.detection.INetworkServiceDetectorProvider;
import com.netifera.platform.net.services.detection.INetworkServiceTrigger;
import com.netifera.platform.net.services.detection.XMLPatternsLoader;
import com.netifera.platform.net.services.detection.XMLTriggersLoader;

public class XMLDetectorProvider implements INetworkServiceDetectorProvider {
	private List<INetworkServiceDetector> clientDetectors;
	private List<INetworkServiceDetector> serverDetectors;
	private List<INetworkServiceTrigger> serviceTriggers;

	protected void activate(ComponentContext context) {
		InputStream stream;
		try {
			stream = context.getBundleContext().getBundle().getEntry("ServerPatterns.xml").openStream();
			XMLPatternsLoader reader = new XMLPatternsLoader(stream);
			serverDetectors = reader.getDetectors();
			stream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			stream = context.getBundleContext().getBundle().getEntry("ClientPatterns.xml").openStream();
			XMLPatternsLoader reader = new XMLPatternsLoader(stream);
			clientDetectors = reader.getDetectors();
			stream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			stream = context.getBundleContext().getBundle().getEntry("Triggers.xml").openStream();
			XMLTriggersLoader  reader = new XMLTriggersLoader(stream);
			serviceTriggers = reader.getTriggers();
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