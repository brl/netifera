package com.maxmind.geoip.internal;


import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.service.component.ComponentContext;

import com.maxmind.geoip.LookupService;
import com.netifera.platform.api.log.ILogManager;
import com.netifera.platform.api.log.ILogger;
import com.netifera.platform.net.routes.AS;
import com.netifera.platform.net.routes.IIP2ASService;
import com.netifera.platform.util.addresses.inet.InternetAddress;
import com.netifera.platform.util.addresses.inet.InternetNetblock;

public class IP2ASService implements IIP2ASService {
	static final private String DB_FILENAME = "GeoIPASNum.dat";
	
	private LookupService lookupService;
	
	private ILogger logger;
	
	private Pattern asnumPattern = Pattern.compile("AS([\\d]+)[^\\d]*");
	
	public synchronized AS getAS(InternetAddress address) {
		if (lookupService == null)
			return null; // initialization failed, for example the db file was not found
		
		final String as = lookupService.getOrg(address.toInetAddress());
		if (as == null)
			return null;
		return new AS() {
			public String getDescription() {
				return as;
			}
			public long getNumber() {
				Matcher matcher = asnumPattern.matcher(as);
				if (matcher.matches()) {
					String asnum = matcher.group(1);
					if (asnum != null && asnum.length()>0)
						return Long.parseLong(asnum);
				}
				return 0;
			}
		};
	}
	
	public synchronized AS getAS(InternetNetblock netblock) {
		//FIXME this could probably be made more accurate exploiting the internal structure of the maxmind database, maybe it contains the BGP prefix
		
		AS as = getAS(netblock.get(0));
		if (as == null)
			return null;
		long number = as.getNumber();
		if (number == 0)
			return null;
		AS as2 = getAS(netblock.get(netblock.size()/2));
		if (as2 == null || number != as2.getNumber())
			return null;
		AS as3 = getAS(netblock.get(netblock.size()-1));
		if (as3 == null || number != as3.getNumber())
			return null;

		return as;
	}

	
	protected void activate(ComponentContext context) {
		if (lookupService == null)
			try {
				String path = getDBPath();
				verifyDBPath(path);
				lookupService = new LookupService(path, LookupService.GEOIP_MEMORY_CACHE);
			} catch (IOException e) {
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
	}
	
	private boolean isRunningInEclipse() {
		return System.getProperty("osgi.dev") != null;
	}
	
	protected void setLogManager(ILogManager logManager) {
		this.logger = logManager.getLogger("IP2AS Service");
	}
	
	protected void unsetLogManager(ILogManager logManager) {
	}
}
