package com.netifera.platform.internal.system.privd.messages;

import java.nio.ByteBuffer;

public class MessageResponseFd extends AbstractResponse {

	private int receivedFd = -1;
	
	MessageResponseFd(ByteBuffer buffer)
			throws MessageException {
		super(ResponseType.PRIVD_RESPONSE_FD, buffer);
	}

	void setReceivedFd(int fd) {
		receivedFd = fd;
	}
	
	protected void parseArguments() {
	}
	
	public int getReceivedFd() {
		return receivedFd;
	}

}
