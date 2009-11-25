package com.maxmind.geoip.internal;


import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.osgi.service.component.ComponentContext;

import com.maxmind.geoip.Location;
import com.maxmind.geoip.LookupService;
import com.netifera.platform.api.log.ILogManager;
import com.netifera.platform.api.log.ILogger;
import com.netifera.platform.net.geoip.IGeoIPService;
import com.netifera.platform.net.geoip.ILocation;
import com.netifera.platform.util.addresses.inet.InternetAddress;
import com.netifera.platform.util.addresses.inet.InternetNetblock;

public class GeoIPService implements IGeoIPService {
	static final private String DB_FILENAME = "GeoLiteCity.dat";
	
	private LookupService lookupService;
	
	private ILogger logger;

	private Map<String,String> continentToName = new HashMap<String,String>();
	private Map<String,String> countryToContinent = new HashMap<String,String>();
	
	{
		continentToName.put("AF", "Africa");
		continentToName.put("AS", "Asia");
		continentToName.put("EU", "Europe");
		continentToName.put("NA", "North America");
		continentToName.put("SA", "South America");
		continentToName.put("OC", "Oceania");
		continentToName.put("AN", "Antarctica");
		
		countryToContinent.put("AF", "AS");
		countryToContinent.put("AX", "EU");
		countryToContinent.put("AL", "EU");
		countryToContinent.put("DZ", "AF");
		countryToContinent.put("AS", "OC");
		countryToContinent.put("AD", "EU");
		countryToContinent.put("AO", "AF");
		countryToContinent.put("AI", "NA");
		countryToContinent.put("AQ", "AN");
		countryToContinent.put("AG", "NA");
		countryToContinent.put("AR", "SA");
		countryToContinent.put("AM", "AS");
		countryToContinent.put("AW", "NA");
		countryToContinent.put("AU", "OC");
		countryToContinent.put("AT", "EU");
		countryToContinent.put("AZ", "AS");
		countryToContinent.put("BS", "NA");
		countryToContinent.put("BH", "AS");
		countryToContinent.put("BD", "AS");
		countryToContinent.put("BB", "NA");
		countryToContinent.put("BY", "EU");
		countryToContinent.put("BE", "EU");
		countryToContinent.put("BZ", "NA");
		countryToContinent.put("BJ", "AF");
		countryToContinent.put("BM", "NA");
		countryToContinent.put("BT", "AS");
		countryToContinent.put("BO", "SA");
		countryToContinent.put("BA", "EU");
		countryToContinent.put("BW", "AF");
		countryToContinent.put("BV", "AN");
		countryToContinent.put("BR", "SA");
		countryToContinent.put("IO", "AS");
		countryToContinent.put("VG", "NA");
		countryToContinent.put("BN", "AS");
		countryToContinent.put("BG", "EU");
		countryToContinent.put("BF", "AF");
		countryToContinent.put("BI", "AF");
		countryToContinent.put("KH", "AS");
		countryToContinent.put("CM", "AF");
		countryToContinent.put("CA", "NA");
		countryToContinent.put("CV", "AF");
		countryToContinent.put("KY", "NA");
		countryToContinent.put("CF", "AF");
		countryToContinent.put("TD", "AF");
		countryToContinent.put("CL", "SA");
		countryToContinent.put("CN", "AS");
		countryToContinent.put("CX", "AS");
		countryToContinent.put("CC", "AS");
		countryToContinent.put("CO", "SA");
		countryToContinent.put("KM", "AF");
		countryToContinent.put("CD", "AF");
		countryToContinent.put("CG", "AF");
		countryToContinent.put("CK", "OC");
		countryToContinent.put("CR", "NA");
		countryToContinent.put("CI", "AF");
		countryToContinent.put("HR", "EU");
		countryToContinent.put("CU", "NA");
		countryToContinent.put("CY", "AS");
		countryToContinent.put("CZ", "EU");
		countryToContinent.put("DK", "EU");
		countryToContinent.put("DJ", "AF");
		countryToContinent.put("DM", "NA");
		countryToContinent.put("DO", "NA");
		countryToContinent.put("EC", "SA");
		countryToContinent.put("EG", "AF");
		countryToContinent.put("SV", "NA");
		countryToContinent.put("GQ", "AF");
		countryToContinent.put("ER", "AF");
		countryToContinent.put("EE", "EU");
		countryToContinent.put("ET", "AF");
		countryToContinent.put("FO", "EU");
		countryToContinent.put("FK", "SA");
		countryToContinent.put("FJ", "OC");
		countryToContinent.put("FI", "EU");
		countryToContinent.put("FR", "EU");
		countryToContinent.put("GF", "SA");
		countryToContinent.put("PF", "OC");
		countryToContinent.put("TF", "AN");
		countryToContinent.put("GA", "AF");
		countryToContinent.put("GM", "AF");
		countryToContinent.put("GE", "AS");
		countryToContinent.put("DE", "EU");
		countryToContinent.put("GH", "AF");
		countryToContinent.put("GI", "EU");
		countryToContinent.put("GR", "EU");
		countryToContinent.put("GL", "NA");
		countryToContinent.put("GD", "NA");
		countryToContinent.put("GP", "NA");
		countryToContinent.put("GU", "OC");
		countryToContinent.put("GT", "NA");
		countryToContinent.put("GG", "EU");
		countryToContinent.put("GN", "AF");
		countryToContinent.put("GW", "AF");
		countryToContinent.put("GY", "SA");
		countryToContinent.put("HT", "NA");
		countryToContinent.put("HM", "AN");
		countryToContinent.put("VA", "EU");
		countryToContinent.put("HN", "NA");
		countryToContinent.put("HK", "AS");
		countryToContinent.put("HU", "EU");
		countryToContinent.put("IS", "EU");
		countryToContinent.put("IN", "AS");
		countryToContinent.put("ID", "AS");
		countryToContinent.put("IR", "AS");
		countryToContinent.put("IQ", "AS");
		countryToContinent.put("IE", "EU");
		countryToContinent.put("IM", "EU");
		countryToContinent.put("IL", "AS");
		countryToContinent.put("IT", "EU");
		countryToContinent.put("JM", "NA");
		countryToContinent.put("JP", "AS");
		countryToContinent.put("JE", "EU");
		countryToContinent.put("JO", "AS");
		countryToContinent.put("KZ", "AS");
		countryToContinent.put("KE", "AF");
		countryToContinent.put("KI", "OC");
		countryToContinent.put("KP", "AS");
		countryToContinent.put("KR", "AS");
		countryToContinent.put("KW", "AS");
		countryToContinent.put("KG", "AS");
		countryToContinent.put("LA", "AS");
		countryToContinent.put("LV", "EU");
		countryToContinent.put("LB", "AS");
		countryToContinent.put("LS", "AF");
		countryToContinent.put("LR", "AF");
		countryToContinent.put("LY", "AF");
		countryToContinent.put("LI", "EU");
		countryToContinent.put("LT", "EU");
		countryToContinent.put("LU", "EU");
		countryToContinent.put("MO", "AS");
		countryToContinent.put("MK", "EU");
		countryToContinent.put("MG", "AF");
		countryToContinent.put("MW", "AF");
		countryToContinent.put("MY", "AS");
		countryToContinent.put("MV", "AS");
		countryToContinent.put("ML", "AF");
		countryToContinent.put("MT", "EU");
		countryToContinent.put("MH", "OC");
		countryToContinent.put("MQ", "NA");
		countryToContinent.put("MR", "AF");
		countryToContinent.put("MU", "AF");
		countryToContinent.put("YT", "AF");
		countryToContinent.put("MX", "NA");
		countryToContinent.put("FM", "OC");
		countryToContinent.put("MD", "EU");
		countryToContinent.put("MC", "EU");
		countryToContinent.put("MN", "AS");
		countryToContinent.put("ME", "EU");
		countryToContinent.put("MS", "NA");
		countryToContinent.put("MA", "AF");
		countryToContinent.put("MZ", "AF");
		countryToContinent.put("MM", "AS");
		countryToContinent.put("NA", "AF");
		countryToContinent.put("NR", "OC");
		countryToContinent.put("NP", "AS");
		countryToContinent.put("AN", "NA");
		countryToContinent.put("NL", "EU");
		countryToContinent.put("NC", "OC");
		countryToContinent.put("NZ", "OC");
		countryToContinent.put("NI", "NA");
		countryToContinent.put("NE", "AF");
		countryToContinent.put("NG", "AF");
		countryToContinent.put("NU", "OC");
		countryToContinent.put("NF", "OC");
		countryToContinent.put("MP", "OC");
		countryToContinent.put("NO", "EU");
		countryToContinent.put("OM", "AS");
		countryToContinent.put("PK", "AS");
		countryToContinent.put("PW", "OC");
		countryToContinent.put("PS", "AS");
		countryToContinent.put("PA", "NA");
		countryToContinent.put("PG", "OC");
		countryToContinent.put("PY", "SA");
		countryToContinent.put("PE", "SA");
		countryToContinent.put("PH", "AS");
		countryToContinent.put("PN", "OC");
		countryToContinent.put("PL", "EU");
		countryToContinent.put("PT", "EU");
		countryToContinent.put("PR", "NA");
		countryToContinent.put("QA", "AS");
		countryToContinent.put("RE", "AF");
		countryToContinent.put("RO", "EU");
		countryToContinent.put("RU", "EU");
		countryToContinent.put("RW", "AF");
		countryToContinent.put("BL", "NA");
		countryToContinent.put("SH", "AF");
		countryToContinent.put("KN", "NA");
		countryToContinent.put("LC", "NA");
		countryToContinent.put("MF", "NA");
		countryToContinent.put("PM", "NA");
		countryToContinent.put("VC", "NA");
		countryToContinent.put("WS", "OC");
		countryToContinent.put("SM", "EU");
		countryToContinent.put("ST", "AF");
		countryToContinent.put("SA", "AS");
		countryToContinent.put("SN", "AF");
		countryToContinent.put("RS", "EU");
		countryToContinent.put("SC", "AF");
		countryToContinent.put("SL", "AF");
		countryToContinent.put("SG", "AS");
		countryToContinent.put("SK", "EU");
		countryToContinent.put("SI", "EU");
		countryToContinent.put("SB", "OC");
		countryToContinent.put("SO", "AF");
		countryToContinent.put("ZA", "AF");
		countryToContinent.put("GS", "AN");
		countryToContinent.put("ES", "EU");
		countryToContinent.put("LK", "AS");
		countryToContinent.put("SD", "AF");
		countryToContinent.put("SR", "SA");
		countryToContinent.put("SJ", "EU");
		countryToContinent.put("SZ", "AF");
		countryToContinent.put("SE", "EU");
		countryToContinent.put("CH", "EU");
		countryToContinent.put("SY", "AS");
		countryToContinent.put("TW", "AS");
		countryToContinent.put("TJ", "AS");
		countryToContinent.put("TZ", "AF");
		countryToContinent.put("TH", "AS");
		countryToContinent.put("TL", "AS");
		countryToContinent.put("TG", "AF");
		countryToContinent.put("TK", "OC");
		countryToContinent.put("TO", "OC");
		countryToContinent.put("TT", "NA");
		countryToContinent.put("TN", "AF");
		countryToContinent.put("TR", "AS");
		countryToContinent.put("TM", "AS");
		countryToContinent.put("TC", "NA");
		countryToContinent.put("TV", "OC");
		countryToContinent.put("UG", "AF");
		countryToContinent.put("UA", "EU");
		countryToContinent.put("AE", "AS");
		countryToContinent.put("GB", "EU");
		countryToContinent.put("US", "NA");
		countryToContinent.put("UM", "OC");
		countryToContinent.put("VI", "NA");
		countryToContinent.put("UY", "SA");
		countryToContinent.put("UZ", "AS");
		countryToContinent.put("VU", "OC");
		countryToContinent.put("VE", "SA");
		countryToContinent.put("VN", "AS");
		countryToContinent.put("WF", "OC");
		countryToContinent.put("EH", "AF");
		countryToContinent.put("YE", "AS");
		countryToContinent.put("ZM", "AF");
		countryToContinent.put("ZW", "AF");
	}
	
	public synchronized ILocation getLocation(InternetAddress address) {
		if (lookupService == null)
			return null; // initialization failed, for example the db file was not found
		
		final Location location = lookupService.getLocation(address.toInetAddress());
		if (location == null)
			return null;
		return new ILocation() {
			public double[] getPosition() {
				return new double[] {location.latitude, location.longitude};
			}
			public String getContinent() {
				String continentCode = getContinentCode();
				return continentCode == null ? null : continentToName.get(continentCode);
			}
			public String getContinentCode() {
				return  location.countryCode == null ? null : countryToContinent.get(location.countryCode);
			}
			public String getCountry() {
				return location.countryName;
			}
			public String getCountryCode() {
				return location.countryCode;
			}
			public String getCity() {
				return location.city;
			}
			public String getPostalCode() {
				return location.postalCode;
			}
		};
	}

	public synchronized ILocation getLocation(InternetNetblock netblock) {
		//FIXME this could probably be made more accurate exploiting the internal structure of the maxmind database
		
		if (netblock.getCIDR() < 16) // dont guess the geographical location of too big netblocks
			return null;
		
		final ILocation location = getLocation(netblock.itemAt(0));
		if (location == null)
			return null;
		final ILocation location2 = getLocation(netblock.itemAt(netblock.itemCount()/2));
		if (location2 == null)
			return null;
		final ILocation location3 = getLocation(netblock.itemAt(netblock.itemCount()-1));
		if (location3 == null)
			return null;

		// at least should be the same continent, otherwise not the same location and return null
		String continent = location.getContinentCode();
		if (continent == null)
			return null;
		String continent2 = location2.getContinentCode();
		if (continent2 == null || !continent2.equals(continent))
			return null;
		String continent3 = location3.getContinentCode();
		if (continent3 == null || !continent3.equals(continent))
			return null;

		return new ILocation() {
			public double[] getPosition() {
				// this can be inaccurate
				return location.getPosition();
			}
			public String getContinent() {
				return location.getContinent();
			}
			public String getContinentCode() {
				return location.getContinentCode();
			}
			public String getCountry() {
				String country = location.getCountry();
				if (country == null)
					return null;
				String country2 = location2.getCountry();
				if (country2 == null || !country2.equals(country))
					return null;
				String country3 = location3.getCountry();
				if (country3 == null || !country3.equals(country))
					return null;
				return country;
			}
			public String getCountryCode() {
				String countryCode = location.getCountryCode();
				if (countryCode == null)
					return null;
				String countryCode2 = location2.getCountryCode();
				if (countryCode2 == null || !countryCode2.equals(countryCode))
					return null;
				String countryCode3 = location3.getCountryCode();
				if (countryCode3 == null || !countryCode3.equals(countryCode))
					return null;
				return countryCode;
			}
			public String getCity() {
				String city = location.getCity();
				if (city == null)
					return null;
				String city2 = location2.getCity();
				if (city2 == null || !city2.equals(city))
					return null;
				String city3 = location3.getCity();
				if (city3 == null || !city3.equals(city))
					return null;
				return city;
			}
			public String getPostalCode() {
				String postalCode = location.getPostalCode();
				if (postalCode == null)
					return null;
				String postalCode2 = location2.getPostalCode();
				if (postalCode2 == null || !postalCode2.equals(postalCode))
					return null;
				String postalCode3 = location3.getPostalCode();
				if (postalCode3 == null || !postalCode3.equals(postalCode))
					return null;
				return postalCode;
			}
		};
	}

	
	protected void activate(ComponentContext context) {
		if (lookupService == null)
			try {
				String path = getDBPath();
				verifyDBPath(path);
				lookupService = new LookupService(path, LookupService.GEOIP_MEMORY_CACHE);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}
	
	protected void deactivate(ComponentContext context) {
		if (lookupService != null) {
			try {
				lookupService.close();
			} finally {
				lookupService = null;
			}
		}
	}

	private String getDBPath() {
		if(isRunningInEclipse()) {
			return getEclipseDBPath();
		} else {
			return getBuildDBPath();
		}
	}
	
	private String getEclipseDBPath() {
		final String basePath = getBasePathForEclipse();
		if(basePath == null) {
			logger.error("Could not locate base path for DB binary.");
			return null;
		}
		return basePath + DB_FILENAME;
	}

	private String getBuildDBPath() {
		final String basePath = getBasePathForBuild();
		if(basePath == null) {
			logger.error("Could not locate base path for DB binary.");
			return null;
		}
		return basePath + DB_FILENAME;
	}
	
	private boolean verifyDBPath(String path) {
		if(path == null) { 
			return false;
		}
		File file = new File(path);
		if (!file.exists()) {
			logger.info("DB binary not found at " + path);
			return false;
		} 
		logger.info("DB binary located at " + path);
		return true;
	}
	
	private String getBasePathForBuild() {
		final String installArea =  System.getProperty("osgi.install.area");
		if(installArea == null || !installArea.startsWith("file:")) {
			return null;
		}
		return installArea.substring(5) + "data/";
	}
	
	private String getBasePathForEclipse() {
		return System.getProperty("user.home", System.getenv("HOME")) + File.separator + ".netifera" + File.separator + "data" + File.separator;
/*		final String configArea = System.getProperty("osgi.configuration.area");
		if(configArea == null || !configArea.startsWith("file:")) {
			return null;
		}
		final String trimmedPath = configArea.substring(5);
		int metadataIndex = trimmedPath.indexOf(".metadata");
		if(metadataIndex == -1)
			return null;
		return trimmedPath.substring(0, metadataIndex);
*/	}
	
	private boolean isRunningInEclipse() {
		return System.getProperty("osgi.dev") != null;
	}
	
	protected void setLogManager(ILogManager logManager) {
		this.logger = logManager.getLogger("GeoIP Service");
	}
	
	protected void unsetLogManager(ILogManager logManager) {
	}
}
