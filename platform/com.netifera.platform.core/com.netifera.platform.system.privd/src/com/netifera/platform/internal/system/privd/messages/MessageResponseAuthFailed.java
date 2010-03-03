package com.netifera.platform.internal.system.privd.messages;

import java.nio.ByteBuffer;

public class MessageResponseAuthFailed extends AbstractResponse {
	
	MessageResponseAuthFailed(ByteBuffer buffer) throws MessageException {
		super(ResponseType.PRIVD_RESPONSE_AUTH_FAILED, buffer);
	}
	
	protected void parseArguments() {
		
	}

}
