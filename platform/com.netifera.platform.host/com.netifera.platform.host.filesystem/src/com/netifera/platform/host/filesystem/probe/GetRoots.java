package com.netifera.platform.host.filesystem.probe;

import java.net.URI;

import com.netifera.platform.api.dispatcher.ProbeMessage;
import com.netifera.platform.host.filesystem.File;

public class GetRoots extends ProbeMessage {
	
	private static final long serialVersionUID = 3191360401313277253L;

	public static final String ID = "GetRoots";

	private final URI url;
	private final File[] roots;
	
	GetRoots(URI url) {
		super(ID);
		this.url = url;
		roots = null;
	}
	
	GetRoots createResponse(File[] roots) {
		return new GetRoots(roots, getSequenceNumber());
	}
	
	private GetRoots(File[] roots, int sequenceNumber) {
		super(ID);
		this.url = null;
		this.roots = roots;
		setSequenceNumber(sequenceNumber);
		markAsResponse();
	}

	public URI getFileSystemURL() {
		return url;
	}

	File[] getFileRoots() {
		return roots;
	}
}
