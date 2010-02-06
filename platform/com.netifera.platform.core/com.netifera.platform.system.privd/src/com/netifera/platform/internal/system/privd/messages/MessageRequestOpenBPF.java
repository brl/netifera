package com.netifera.platform.internal.system.privd.messages;

public class MessageRequestOpenBPF extends AbstractRequest {

	MessageRequestOpenBPF() {
		super(RequestType.PRIVD_OPEN_BPF);
	}

	protected void addArguments() {
		// nothing to add
	}

}
