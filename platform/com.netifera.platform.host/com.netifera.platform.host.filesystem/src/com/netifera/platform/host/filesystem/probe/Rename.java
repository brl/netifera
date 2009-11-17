package com.netifera.platform.host.filesystem.probe;

import java.net.URI;

import com.netifera.platform.api.dispatcher.ProbeMessage;

public class Rename extends ProbeMessage {
	
	private static final long serialVersionUID = 3095300374099820256L;

	public static final String ID = "Rename";

	private final URI url;
	private final String oldName;
	private final String newName;
	private final Boolean result;
	
	Rename(URI url, String oldName, String newName) {
		super(ID);
		this.url = url;
		this.oldName = oldName;
		this.newName = newName;
		this.result = null;
	}
	
	Rename createResponse(boolean result) {
		return new Rename(result, getSequenceNumber());
	}
	
	private Rename(boolean result, int sequenceNumber) {
		super(ID);
		this.url = null;
		this.oldName = this.newName = null;
		this.result = result;
		setSequenceNumber(sequenceNumber);
		markAsResponse();
	}

	public URI getFileSystemURL() {
		return url;
	}

	public String getOldName() {
		return oldName;
	}

	public String getNewName() {
		return newName;
	}

	public boolean getResult() {
		return result;
	}
}
