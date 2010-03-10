package com.netifera.platform.net.http.internal.spider.daemon.remote;

import java.util.Set;

import com.netifera.platform.net.http.spider.impl.WebSite;

public class WebSpiderConfiguration {
	public Set<String> modules;
	public int queueSize;
	public int bufferSize;
	public int maximumConnections;
	public boolean followLinks;
	public boolean fetchImages;
	
	public Set<WebSite> targets;
}
