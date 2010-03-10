package com.netifera.platform.net.ui.internal.geoip;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import com.netifera.platform.api.model.AbstractEntity;
import com.netifera.platform.api.model.IEntity;
import com.netifera.platform.api.model.layers.IGroupLayer;
import com.netifera.platform.net.model.HostEntity;
import com.netifera.platform.net.ui.geoip.IGeoIPService;
import com.netifera.platform.net.ui.geoip.ILocation;
import com.netifera.platform.util.addresses.inet.InternetAddress;

public class CountryGroupLayer implements IGroupLayer {

	private IGeoIPService geoipService;
	
	public Set<String> getGroups(IEntity entity) {
		if (entity instanceof HostEntity) {
			String countryCode = ((AbstractEntity)entity).getAttribute("country");
			if (countryCode == null && geoipService != null) {
				InternetAddress address = (InternetAddress) entity.getAdapter(InternetAddress.class);
				if (address != null) {
					ILocation location = geoipService.getLocation(address);
					if (location != null) {
						countryCode = location.getCountryCode();
					}
				}
			}
			if (countryCode != null) {
				Locale locale = new Locale("en", countryCode);
				Set<String> answer = new HashSet<String>();
				answer.add(locale.getDisplayCountry(Locale.ENGLISH));
				return answer;
			}
		}
		return Collections.emptySet(); // or return "Unknown Country"
	}
	
	public String getName() {
		return "Hosts By Country";
	}

	public boolean isDefaultEnabled() {
		return false;
	}
	
	protected void setGeoIPService(IGeoIPService service) {
		this.geoipService = service;
	}
	
	protected void unsetGeoIPService(IGeoIPService service) {
		this.geoipService = null;
	}
}
