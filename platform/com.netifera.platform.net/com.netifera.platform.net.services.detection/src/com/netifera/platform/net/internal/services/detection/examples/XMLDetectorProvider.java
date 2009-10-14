package com.netifera.platform.net.internal.services.detection.examples;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.ComponentContext;

import com.netifera.platform.net.services.detection.INetworkServiceDetector;
import com.netifera.platform.net.services.detection.INetworkServiceDetectorProvider;
import com.netifera.platform.net.services.detection.INetworkServiceTrigger;
import com.netifera.platform.util.HexaEncoding;
import com.netifera.platform.util.PortSet;
import com.netifera.platform.util.patternmatching.ISessionPattern;
import com.netifera.platform.util.patternmatching.Regex;
import com.netifera.platform.util.patternmatching.SessionPattern;
import com.netifera.platform.util.xml.XMLElement;
import com.netifera.platform.util.xml.XMLParseException;

public class XMLDetectorProvider implements INetworkServiceDetectorProvider {
	private static final String PATTERN_TAG = "ServicePattern";
	private static final String PATTERN_TAG_ATT_REGEX = "regex";
	private static final String SERVICE_TAG  = "service";
	private static final String REGEX_GROUP_PREFIX  = "$regex-group-";

	private static final String TRIGGER_TAG = "ServiceTrigger";
	private static final String TRIGGER_TAG_ATT_NAME = "name";
	private static final String TRIGGER_TAG_ATT_DATA = "data";
	private static final String TRIGGER_TAG_ATT_TIMEOUT = "timeout";
	private static final String ENDPOINT_TAG = "Endpoint";
	private static final String ENDPOINT_TAG_ATT_PROTOCOL = "protocol";

	private List<INetworkServiceDetector> clientDetectors;
	private List<INetworkServiceDetector> serverDetectors;
	private List<INetworkServiceTrigger> serviceTriggers;

	protected void activate(ComponentContext context) {
		InputStream stream;
		try {
			stream = context.getBundleContext().getBundle().getEntry("ServerPatterns.xml").openStream();
			serverDetectors = loadDetectors(stream);
			stream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			stream = context.getBundleContext().getBundle().getEntry("ClientPatterns.xml").openStream();
			clientDetectors = loadDetectors(stream);
			stream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			stream = context.getBundleContext().getBundle().getEntry("Triggers.xml").openStream();
			serviceTriggers = loadTriggers(stream);
			stream.close();
		}	catch(Exception e) {
			e.printStackTrace();
		}
	}

	protected void deactivate(ComponentContext context) {
	}

	private List<INetworkServiceDetector> loadDetectors(InputStream stream) {
		List<INetworkServiceDetector> answer = new ArrayList<INetworkServiceDetector>();
		InputStreamReader reader = new InputStreamReader(stream);
		XMLElement xml = new XMLElement();
		try {
			xml.parseFromReader(reader);
			for (XMLElement patternElement: xml.getChildren()) {
				if (!patternElement.getName().equals(PATTERN_TAG)) {
					System.err.println("XMLDetectorProvider: Tag "+PATTERN_TAG+" expected, got "+patternElement.getName()+" instead!");
					continue;
				}
				Regex regex = new Regex(patternElement.getStringAttribute(PATTERN_TAG_ATT_REGEX));
				String service = null;
				for (XMLElement child: patternElement.getChildren()) {
					String name = child.getName();
					String value = child.getContent();
					
					if (name.equals(SERVICE_TAG)) {
						service = value;
						regex.add("serviceType", service); //FIXME serviceType deprecated
					}
					if(value.startsWith(REGEX_GROUP_PREFIX)) {
						Integer groupIndex = Integer.valueOf(value.substring(REGEX_GROUP_PREFIX.length()));
						regex.add(groupIndex, name);
					} else {
						regex.add(name, value);
					}
				}
				INetworkServiceDetector detector = newDetector(service, new SessionPattern(new Regex(".*"), regex));
				answer.add(detector);
			}
		} catch (XMLParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return answer;
	}

	private static INetworkServiceDetector newDetector(final String service, final ISessionPattern pattern) {
		return new INetworkServiceDetector() {
			public Map<String, String> detect(String clientData,
					String serverData) {
				return pattern.match(clientData, serverData);
			}
			public PortSet getPorts() {
				return null;
			}
			public String getProtocol() {
				return null;
			}
			@Override
			public String toString() {
				return /*ports.toString() + "/" + protocol + "\n" +*/ pattern.toString();
			}
		};
	}

	private List<INetworkServiceTrigger> loadTriggers(InputStream stream) {
		List<INetworkServiceTrigger> answer = new ArrayList<INetworkServiceTrigger>();
		InputStreamReader reader = new InputStreamReader(stream);
		XMLElement xml = new XMLElement();
		try {
			xml.parseFromReader(reader);
			for (XMLElement triggerElement: xml.getChildren()) {
				if (!triggerElement.getName().equals(TRIGGER_TAG)) {
					System.err.println("XMLDetectorProvider: Tag "+TRIGGER_TAG+" expected, got "+triggerElement.getName()+" instead!");
					continue;
				}
				
				String name = triggerElement.getStringAttribute(TRIGGER_TAG_ATT_NAME);
				byte[] data = HexaEncoding.hex2bytes(triggerElement.getStringAttribute(TRIGGER_TAG_ATT_DATA));
				int timeout = triggerElement.getIntAttribute(TRIGGER_TAG_ATT_TIMEOUT);

				for (XMLElement endpointElement: triggerElement.getChildren()) {
					if (!endpointElement.getName().equals(ENDPOINT_TAG)) {
						System.err.println("XMLDetectorProvider: Tag "+ENDPOINT_TAG+" expected, got "+endpointElement.getName()+" instead!");
						continue;
					}

					String protocol = endpointElement.getStringAttribute(ENDPOINT_TAG_ATT_PROTOCOL);
					PortSet ports = new PortSet(endpointElement.getContent());
					INetworkServiceTrigger trigger = newTrigger(name, timeout, protocol, ports, data);
					answer.add(trigger);
				}
			}
		} catch (XMLParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return answer;
	}

	private static INetworkServiceTrigger newTrigger(final String name, final int timeout, final String protocol, final PortSet ports, final byte[] bytes) {
		return new INetworkServiceTrigger() {
			public String getName() {
				return name;
			}
			public int getTimeout() {
				return timeout;
			}
			public byte[] getBytes() {
				return bytes;
			}
			public PortSet getPorts() {
				return ports;
			}
			public String getProtocol() {
				return protocol;
			}
			@Override
			public String toString() {
				return name+" "+timeout+" "+ports.toString() + "/" + protocol + " " + HexaEncoding.bytes2hex(bytes);
			}
		};
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