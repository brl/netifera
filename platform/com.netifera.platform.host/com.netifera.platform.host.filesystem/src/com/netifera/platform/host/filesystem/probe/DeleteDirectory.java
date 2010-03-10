package com.netifera.platform.host.filesystem.probe;

import java.net.URI;

import com.netifera.platform.api.dispatcher.ProbeMessage;

public class DeleteDirectory extends ProbeMessage {
	
	private static final long serialVersionUID = -4656707105181094706L;

	public static final String ID = "DeleteDirectory";

	private final URI url;
	private final String path;
	private final Boolean result;
	
	DeleteDirectory(URI url, String path) {
		super(ID);
		this.url = url;
		this.path = path;
		this.result = null;
	}
	
	DeleteDirectory createResponse(boolean result) {
		return new DeleteDirectory(result, getSequenceNumber());
	}
	
	private DeleteDirectory(boolean result, int sequenceNumber) {
		super(ID);
		this.url = null;
		this.path = null;
		this.result = result;
		setSequenceNumber(sequenceNumber);
		markAsResponse();
	}

	public URI getFileSystemURL() {
		return url;
	}

	public String getPath() {
		return path;
	}
	
	public boolean getResult() {
		return result;
	}
}
