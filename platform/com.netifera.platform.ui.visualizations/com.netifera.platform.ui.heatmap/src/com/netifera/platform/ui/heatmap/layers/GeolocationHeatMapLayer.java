package com.netifera.platform.ui.heatmap.layers;

import com.netifera.platform.net.ui.geoip.ILocation;
import com.netifera.platform.ui.internal.heatmap.Activator;
import com.netifera.platform.util.addresses.inet.IPv4Netblock;


public class GeolocationHeatMapLayer extends AbstractXKCDHeatMapLayer {
	
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
	
	public String getName() {
		return "Geolocation";
	}

	public boolean isDefaultEnabled() {
		return true;
	}
}