package com.netifera.platform.host.filesystem.probe;

import java.net.URI;

import com.netifera.platform.api.dispatcher.ProbeMessage;
import com.netifera.platform.host.filesystem.File;

public class CreateDirectory extends ProbeMessage {
	
	private static final long serialVersionUID = -1796359390278969145L;

	public static final String ID = "CreateDirectory";

	private final URI url;
	private final String path;
	private final File result;
	
	CreateDirectory(URI url, String path) {
		super(ID);
		this.url = url;
		this.path = path;
		this.result = null;
	}
	
	CreateDirectory createResponse(File result) {
		return new CreateDirectory(result, getSequenceNumber());
	}
	
	private CreateDirectory(File result, int sequenceNumber) {
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
	
	public File getResult() {
		return result;
	}
}
