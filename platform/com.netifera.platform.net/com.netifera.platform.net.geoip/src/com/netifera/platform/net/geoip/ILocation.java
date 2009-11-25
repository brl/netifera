package com.netifera.platform.net.geoip;

public interface ILocation {
	double[] getPosition();
	String getContinent();
	String getContinentCode();
	String getCountry();
	String getCountryCode();
	String getCity();
	String getPostalCode();
}
