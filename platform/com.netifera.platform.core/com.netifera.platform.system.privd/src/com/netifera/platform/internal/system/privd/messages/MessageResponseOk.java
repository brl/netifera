package com.netifera.platform.internal.system.privd.messages;

import java.nio.ByteBuffer;

public class MessageResponseOk extends AbstractResponse {

	MessageResponseOk(ByteBuffer buffer)
			throws MessageException {
		super(ResponseType.PRIVD_RESPONSE_OK, buffer);
	}

	protected void parseArguments() {		
	}

}
