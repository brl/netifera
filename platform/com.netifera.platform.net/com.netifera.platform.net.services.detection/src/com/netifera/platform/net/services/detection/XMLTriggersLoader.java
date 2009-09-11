package com.netifera.platform.net.services.detection;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import com.netifera.platform.util.HexaEncoding;
import com.netifera.platform.util.PortSet;

public class XMLTriggersLoader {
	List<INetworkServiceTrigger>answer =  new ArrayList<INetworkServiceTrigger>();

	/*SAX Parser Handler. No error checking, assumes validated file.*/
	private class SAXPatternHandler extends DefaultHandler {
		private static final String TRIGGER_TAG = "ServiceTrigger";
		private static final String TRIGGER_TAG_ATT_NAME = "name";
		private static final String TRIGGER_TAG_ATT_DATA = "data";
		private static final String TRIGGER_TAG_ATT_TIMEOUT = "timeout";
		private static final String ENDPOINT_TAG = "Endpoint";
		private static final String ENDPOINT_TAG_ATT_PROTOCOL = "protocol";

		String protocol;
		String value;
		String name;
		byte[] bytes;
		int timeout;

		public void startElement(String uri, String localName, String qName,
				Attributes attributes) throws SAXException {

			String name = uri.equals("") ? qName : localName;
			value = "";
			if(name.equals(TRIGGER_TAG)) {
				name = attributes.getValue(TRIGGER_TAG_ATT_NAME);
				bytes = HexaEncoding.hex2bytes(attributes.getValue(TRIGGER_TAG_ATT_DATA));
				timeout = Integer.valueOf(attributes.getValue(TRIGGER_TAG_ATT_TIMEOUT));
			}
			else if(name.equals(ENDPOINT_TAG)) {
				protocol = attributes.getValue(ENDPOINT_TAG_ATT_PROTOCOL);
			}
		}

		public void endElement(String uri, String localName, String qName)
		throws SAXException {
			String name = uri.equals("") ? qName : localName;
			if(name.equals(ENDPOINT_TAG)) {
				answer.add(newTrigger(protocol, new PortSet(value), bytes));
			}
		}

		@Override
		public void characters(char[] ch, int start, int length)
		throws SAXException {
			super.characters(ch, start, length);
			value += new String( ch, start, length );
		}
	};

	public XMLTriggersLoader(InputStream stream) {
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

	private static INetworkServiceTrigger newTrigger(final String protocol, final PortSet ports, final byte[] bytes) {
		return new INetworkServiceTrigger() {
			public String getName() {
				return ""; //TODO
			}
			public int getTimeout() {
				return 0; //TODO
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
				return ports.toString() + "/" + protocol + " " + HexaEncoding.bytes2hex(bytes);
			}
		};
	}
	public List<INetworkServiceTrigger> getTriggers(){
		return answer;
	}
}
