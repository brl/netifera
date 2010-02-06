package com.netifera.platform.internal.system.privd.messages;

import java.nio.ByteBuffer;

public class MessageResponseError extends AbstractResponse {

	private String errorString;
	
	MessageResponseError(ByteBuffer buffer)
			throws MessageException {
		super(ResponseType.PRIVD_RESPONSE_ERROR, buffer);
	}

	protected void parseArguments() {
		errorString = parseStringArgument();		
	}
	
	public String getErrorString() {
		return errorString;
	}

}
