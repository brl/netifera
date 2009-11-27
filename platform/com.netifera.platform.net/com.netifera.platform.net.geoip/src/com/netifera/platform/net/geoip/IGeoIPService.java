package com.netifera.platform.net.geoip;

import com.netifera.platform.util.addresses.inet.InternetAddress;
import com.netifera.platform.util.addresses.inet.InternetNetblock;

public interface IGeoIPService {
	ILocation getLocation(InternetAddress address);
	ILocation getLocation(InternetNetblock netblock);
}
