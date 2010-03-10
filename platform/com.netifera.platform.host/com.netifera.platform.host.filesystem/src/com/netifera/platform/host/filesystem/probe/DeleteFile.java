package com.netifera.platform.host.filesystem.probe;

import java.net.URI;

import com.netifera.platform.api.dispatcher.ProbeMessage;

public class DeleteFile extends ProbeMessage {

	private static final long serialVersionUID = 1012586056677234506L;

	public static final String ID = "DeleteFile";

	private final URI url;
	private final String path;
	private final Boolean result;
	
	DeleteFile(URI url, String path) {
		super(ID);
		this.url = url;
		this.path = path;
		this.result = null;
	}
	
	DeleteFile createResponse(boolean result) {
		return new DeleteFile(result, getSequenceNumber());
	}
	
	private DeleteFile(boolean result, int sequenceNumber) {
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
