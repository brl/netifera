package com.netifera.platform.net.http.spider.impl;

import com.netifera.platform.net.http.service.HTTP;

public class WebSite {
	HTTP http;
	String vhost;

	public WebSite(HTTP http, String vhost) {
		this.http = http;
		this.vhost = vhost;
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof WebSite))
			return false;
		return ((WebSite)o).http.getLocator().equals(http.getLocator()) && ((WebSite)o).vhost.equals(vhost);
	}
	
	@Override
	public int hashCode() {
		return vhost.hashCode();
	}
	
	@Override
	public String toString() {
		return http.getURI(vhost)+" ("+http.getLocator().getAddress()+")";
	}
}
