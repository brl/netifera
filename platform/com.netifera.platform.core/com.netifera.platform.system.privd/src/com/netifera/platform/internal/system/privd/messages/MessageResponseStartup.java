package com.netifera.platform.internal.system.privd.messages;

import java.nio.ByteBuffer;

public class MessageResponseStartup extends AbstractResponse {

	private StartupType startupType;
	private String startupMessage;
	
	MessageResponseStartup(ByteBuffer buffer)
			throws MessageException {
		super(ResponseType.PRIVD_RESPONSE_STARTUP, buffer);
	}

	String getMessage() {
		return startupMessage;
	}
	
	@Override
	protected void parseArguments() {
		int code = parseIntegerArgument();
		startupType = StartupType.fromCode(code);
		if(startupType == null) {
			
		}
		if(hasAnotherArgument()) {
			startupMessage = parseStringArgument();
		}		
	}

}
