package com.netifera.platform.ui.treemap.layers;

import com.netifera.platform.net.geoip.ILocation;
import com.netifera.platform.ui.internal.treemap.Activator;
import com.netifera.platform.util.addresses.inet.IPv4Netblock;


public class GeolocationTreeMapLayer extends AbstractXKCDTreeMapLayer {
	
	protected String getCategory(IPv4Netblock netblock) {
		ILocation location = Activator.getInstance().getGeoIPService().getLocation(netblock);
		if (location == null)
			return null;
		if (netblock.getCIDR() > 16 && location.getCity() != null)
			return location.getCity();
		if (location.getCountry() != null)
			return location.getCountry();
		return location.getContinent();
	}
	
	public String getLayerName() {
		return "Geolocation";
	}

	public boolean isDefaultEnabled() {
		return true;
	}
}