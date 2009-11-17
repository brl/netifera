package com.netifera.platform.host.filesystem.probe;

import java.net.URI;

import com.netifera.platform.api.dispatcher.ProbeMessage;
import com.netifera.platform.host.filesystem.File;

public class GetDirectoryListing extends ProbeMessage {
	
	 
	private static final long serialVersionUID = -9213737500680660072L;

	public static final String ID = "GetDirectoryListing";

	private final URI url;
	private final File[] entries;
	private final String path;
	
	GetDirectoryListing(URI url, String path) {
		super(ID);
		this.url = url;
		this.path = path;
		this.entries = null;
	}
	
	GetDirectoryListing createResponse(File[] entries) {
		return new GetDirectoryListing(entries, getSequenceNumber());
	}
	
	private GetDirectoryListing(File[] entries, int sequenceNumber) {
		super(ID);
		this.entries = entries;
		this.url = null;
		this.path = null;
		setSequenceNumber(sequenceNumber);
		markAsResponse();
	}

	public URI getFileSystemURL() {
		return url;
	}
	
	public String getPath() {
		return path;
	}
	
	public File[] getDirectoryEntries() {
		return entries;
	}
}
