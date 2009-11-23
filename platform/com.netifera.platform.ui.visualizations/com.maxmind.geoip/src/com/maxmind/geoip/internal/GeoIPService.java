package com.maxmind.geoip.internal;


import java.io.File;
import java.io.IOException;

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

	public synchronized ILocation getLocation(InternetAddress address) {
		if (lookupService == null)
			return null; // initialization failed, for example the db file was not found
		
		final Location location = lookupService.getLocation(address.toInetAddress());
		if (location == null)
			return null;
		return new ILocation() {
			public String getCity() {
				return location.city;
			}
			public String getCountry() {
				return location.countryName;
			}
			public String getCountryCode() {
				return location.countryCode;
			}
			public double[] getPosition() {
				return new double[] {location.latitude, location.longitude};
			}
			public String getPostalCode() {
				return location.postalCode;
			}
		};
	}

	public synchronized ILocation getLocation(InternetNetblock netblock) {
		//FIXME this could probably be made more accurate exploiting the internal structure of the maxmind database
		
		if (netblock.getCIDR() < 16) // dont guess the geographical location of big netblocks
			return null;
		
		if (lookupService == null)
			return null; // initialization failed, for example the db file was not found
		
		final Location location = lookupService.getLocation(netblock.itemAt(0).toInetAddress());
		if (location == null)
			return null;
		final Location location2 = lookupService.getLocation(netblock.itemAt(netblock.itemCount()/2).toInetAddress());
		if (location2 == null)
			return null;
		final Location location3 = lookupService.getLocation(netblock.itemAt(netblock.itemCount()-1).toInetAddress());
		if (location3 == null)
			return null;

		// at least should be the same country, otherwise not the same location and return null
		if (location.countryName == null || location2.countryName == null || location3.countryName == null)
			return null;
		if (!location.countryName.equals(location2.countryName) || !location.countryName.equals(location3.countryName))
			return null;

		return new ILocation() {
			public String getCity() {
				if (location.city == null || location2.city == null || location3.city == null)
					return null;
				if (!location.city.equals(location2.city) || !location.city.equals(location3.city))
					return null;
				return location.city;
			}
			public String getCountry() {
				return location.countryName;
			}
			public String getCountryCode() {
				if (location.countryCode == null || location2.countryCode == null || location3.countryCode == null)
					return null;
				if (!location.countryCode.equals(location2.countryCode) || !location.countryCode.equals(location3.countryCode))
					return null;
				return location.countryCode;
			}
			public double[] getPosition() {
				// this can be inaccurate
				return new double[] {location.latitude, location.longitude};
			}
			public String getPostalCode() {
				if (location.postalCode == null || location2.postalCode == null || location3.postalCode == null)
					return null;
				if (!location.postalCode.equals(location2.postalCode) || !location.postalCode.equals(location3.postalCode))
					return null;
				return location.postalCode;
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
