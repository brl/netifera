package com.netifera.platform.net.ui.rir;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.eclipse.jface.action.IAction;
import org.osgi.service.component.ComponentContext;

import com.netifera.platform.api.log.ILogManager;
import com.netifera.platform.api.log.ILogger;
import com.netifera.platform.net.tools.basic.AddNetblocks;
import com.netifera.platform.tools.options.GenericOption;
import com.netifera.platform.ui.actions.ToolAction;
import com.netifera.platform.ui.api.inputbar.IInputBarActionProvider;
import com.netifera.platform.util.addresses.inet.IPv4Address;
import com.netifera.platform.util.addresses.inet.IPv6Address;
import com.netifera.platform.util.addresses.inet.InternetNetblock;

/*
import com.netifera.platform.util.addresses.AddressFormatException;
import com.netifera.platform.util.addresses.inet.InternetAddress;
import com.netifera.platform.util.addresses.inet.InternetNetblock;
import com.netifera.platform.util.addresses.inet.InternetSocketAddress;
import com.netifera.platform.util.patternmatching.InternetAddressMatcher;
import com.netifera.platform.util.patternmatching.NetblockMatcher;
*/

public class InputBarActionProvider implements IInputBarActionProvider {

	private static String DELEGATED_AFRINIC = "delegated-afrinic-latest";
	private static String DELEGATED_APNIC = "delegated-apnic-latest";
	private static String DELEGATED_ARIN = "delegated-arin-latest";
	private static String DELEGATED_LACNIC = "delegated-lacnic-latest";
	private static String DELEGATED_RIPENCC = "delegated-ripencc-latest";

	private static Map<String,String> CODES;
	
	static {
		CODES = new HashMap<String,String>();

		for (String code: new String[] {"AO", "BF", "BI", "BJ", "BW", "CD", "CF", "CG", "CI", "CM", "CV", "DE", "DJ", "DZ", "EG", "ER", "ET", "GA", "GH", "GM", "GN", "GQ", "GW", "KE", "KM", "LR", "LS", "LY", "MA", "MG", "ML", "MR", "MU", "MW", "MZ", "NA", "NE", "NG", "RE", "RW", "SC", "SD", "SL", "SN", "SZ", "TG", "TN", "TZ", "UG", "ZA", "ZM", "ZW"})
			CODES.put(code, DELEGATED_AFRINIC);

		for (String code: new String[] {"AF", "AP", "AS", "AU", "BD", "BN", "BT", "CH", "CK", "CN", "FJ", "FM", "FR", "GB", "GU", "HK", "ID", "IN", "IO", "JP", "KH", "KI", "KP", "KR", "LA", "LK", "MH", "MM", "MN", "MO", "MP", "MU", "MV", "MY", "NC", "NF", "NL", "NP", "NR", "NU", "NZ", "PF", "PG", "PH", "PK", "PW", "SA", "SB", "SE", "SG", "TH", "TL", "TO", "TV", "TW", "US", "VN", "VU", "WF", "WS"})
			CODES.put(code, DELEGATED_APNIC);

		for (String code: new String[] {"AE", "AG", "AI", "AT", "AU", "BB", "BE", "BM", "BS", "CA", "CH", "CZ", "DE", "DM", "ES", "FI", "FR", "GB", "GD", "GP", "HK", "IE", "IL", "IT", "JM", "JP", "KN", "KR", "KY", "LC", "LU", "MF", "MS", "MW", "MX", "NL", "NO", "PM", "PR", "SE", "SG", "TC", "UA", "US", "VC", "VG", "VI"})
			CODES.put(code, DELEGATED_ARIN);

		for (String code: new String[] {"AN", "AR", "AW", "BO", "BR", "BZ", "CL", "CO", "CR", "CU", "DO", "EC", "GF", "GT", "GY", "HN", "HT", "MX", "NI", "PA", "PE", "PY", "SR", "SV", "TT", "UY", "VE"})
			CODES.put(code, DELEGATED_LACNIC);
		
		for (String code: new String[] {"AD", "AE", "AL", "AM", "AT", "AX", "AZ", "BA", "BE", "BG", "BH", "BY", "CH", "CS", "CY", "CZ", "DE", "DK", "EE", "EG", "ES", "EU", "FI", "FO", "FR", "GB", "GE", "GG", "GI", "GL", "GR", "HR", "HU", "IE", "IL", "IM", "IQ", "IR", "IS", "IT", "JE", "JO", "KG", "KW", "KZ", "LB", "LI", "LT", "LU", "LV", "MC", "MD", "ME", "MK", "MT", "NL", "NO", "OM", "PL", "PS", "PT", "QA", "RO", "RS", "RU", "SA", "SE", "SI", "SK", "SM", "SY", "TJ", "TM", "TR", "UA", "UZ", "VA", "YE"})
			CODES.put(code, DELEGATED_RIPENCC);
	}

	private ILogger logger;
	
	public List<IAction> getActions(final long realm, final long spaceId, String input) {
		if (input.length() != 2)
			return Collections.emptyList();
		
		input = input.toUpperCase(Locale.ENGLISH);
		InternetNetblock[] netblocks = getNetblocks(input);
		if (netblocks == null)
			return Collections.emptyList();
		
		List<IAction> actions = new ArrayList<IAction>();
		ToolAction addNetblocks = new ToolAction("Add netblocks for "+input, AddNetblocks.class.getName());
		addNetblocks.addFixedOption(new GenericOption(InternetNetblock[].class, "netblocks", "Netblocks", "Netblocks to add to the model", netblocks));
		actions.add(addNetblocks);

		return actions;
	}

	private InternetNetblock[] getNetblocks(String countryCode) {
		countryCode = countryCode.toUpperCase(Locale.ENGLISH);
		String fileName = CODES.get(countryCode);
		if (fileName == null)
			return null;

		String path = getBasePath()+File.separator+fileName;
		try {
			BufferedReader reader = new BufferedReader(new FileReader(path));
			try {
				List<InternetNetblock> netblocks = new ArrayList<InternetNetblock>();
				while (true) {
					String line = reader.readLine();
					if (line == null) break;
					if (line.length() == 0 || line.startsWith("#"))
						continue;
					String[] parts = line.split("\\|");
					if (parts.length < 5)
						continue;
					if (!parts[1].equals(countryCode))
						continue;
					if (parts[2].equals("ipv4")) {
						IPv4Address startAddress = IPv4Address.fromString(parts[3]);
						int count = Integer.parseInt(parts[4]);
						netblocks.add(InternetNetblock.fromRange(startAddress, count));
					} else if (parts[2].equals("ipv6")) {
						IPv6Address address = IPv6Address.fromString(parts[3]);
						int maskBitCount = Integer.parseInt(parts[4]);
						netblocks.add(InternetNetblock.fromAddress(address, maskBitCount));
					}
				}
				return netblocks.toArray(new InternetNetblock[netblocks.size()]);
			} finally {
				reader.close();
			}
		} catch (FileNotFoundException e) {
			logger.error("RIR report not found", e);
		} catch (IOException e) {
			logger.error("Error while reading RIR report "+fileName, e);
		}
		return null;
	}
	
	private String getBasePath() {
		return System.getProperty("user.home", System.getenv("HOME")) + File.separator + ".netifera" + File.separator + "data" + File.separator + "rir" + File.separator;
	}
	
	protected void activate(ComponentContext context) {
		String path = getBasePath();
		File file = new File(path);
		if (!file.exists()) {
			logger.info("RIR assignment reports not found at " + path);
			logger.info("Please download RIR assignment reports from:\n\tftp://ftp.arin.net/pub/stats/arin/delegated-arin-latest\n\tftp://ftp.ripe.net/ripe/stats/delegated-ripencc-latest\n\tftp://ftp.afrinic.net/pub/stats/afrinic/delegated-afrinic-latest\n\tftp://ftp.apnic.net/pub/stats/apnic/delegated-apnic-latest\n\tftp://ftp.lacnic.net/pub/stats/lacnic/delegated-lacnic-latest");
		} else {
			logger.info("RIR assignment reports located at " + path);
		}
	}

	protected void setLogManager(ILogManager logManager) {
		this.logger = logManager.getLogger("RIR Input Bar Service");
	}
	
	protected void unsetLogManager(ILogManager logManager) {
	}
}
