package com.netifera.platform.net.services.detection;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import com.netifera.platform.util.PortSet;
import com.netifera.platform.util.patternmatching.ISessionPattern;
import com.netifera.platform.util.patternmatching.Regex;
import com.netifera.platform.util.patternmatching.SessionPattern;

@Deprecated
public class XMLPatternsLoader {
	List<INetworkServiceDetector> answer = new ArrayList<INetworkServiceDetector>();

	/*SAX Parser Handler. No error checking, assumes validated file.*/
	private class SAXPatternHandler extends DefaultHandler {
		private static final String PATTERN_TAG = "ServicePattern";
		private static final String PATTERN_TAG_ATT_REGEX = "regex";
		private static final String SERVICE_TAG  = "service";

		Regex pattern;
		String service;
		String value;

		public void startElement(String uri, String localName, String qName,
				Attributes attributes) throws SAXException {
			value = "";
			String name = uri.equals("") ? qName : localName;

			if(name.equals(PATTERN_TAG)) {
				pattern = new Regex(attributes.getValue(PATTERN_TAG_ATT_REGEX));
			}
		}

		public void endElement(String uri, String localName, String qName)
		throws SAXException {
			String name = uri.equals("") ? qName : localName;
			if(name.equals(PATTERN_TAG)) {
				INetworkServiceDetector newDetector = newDetector(service, new SessionPattern(new Regex(".*"),pattern));
				answer.add(newDetector);
				pattern = null;
			} else {
				if(name.equals(SERVICE_TAG)) {
					service = value;
					pattern.add("serviceType", value);
				}
				/*pattern fields tag ends (ex: os,version,service) */
				if (pattern != null) {
					pattern.add(name, value);
				}
			}
		}

		@Override
		public void characters(char[] ch, int start, int length)
		throws SAXException {
			super.characters(ch, start, length);
			String lSpecialCharacters = "\n\t\r";
			for ( int i = start; i < start+length-1; i++ )
			{
				if ( lSpecialCharacters.indexOf( ch[i] ) >= 0 )
				{
					ch[i] = ' ';
				}
			}
			value += new String( ch, start, length );
		}
	};

	public XMLPatternsLoader(InputStream stream) {
		XMLReader xmlReader;
		try {
			xmlReader = XMLReaderFactory.createXMLReader();
			SAXPatternHandler handler = new SAXPatternHandler();
			xmlReader.setContentHandler(handler);
			xmlReader.setErrorHandler(handler);
			Reader r = new InputStreamReader(stream);
			xmlReader.parse(new InputSource(r));
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	private static INetworkServiceDetector newDetector(final String service, final ISessionPattern pattern) {
		//XXX replace by service protocol and ports or endpoints
//		final String protocol = "tcp";
//		final PortSet ports = new PortSet("1-65535");
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

	public List<INetworkServiceDetector> getDetectors() {
		return answer;
	}
}