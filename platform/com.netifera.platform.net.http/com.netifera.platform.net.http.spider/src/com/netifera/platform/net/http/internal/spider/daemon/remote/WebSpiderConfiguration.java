package com.netifera.platform.net.http.internal.spider.daemon.remote;

import java.util.Set;

public class WebSpiderConfiguration {
	public Set<String> modules;
	public int queueSize;
	public int bufferSize;
	public int maximumConnections;
	public boolean followLinks;
	public boolean fetchImages;
}
