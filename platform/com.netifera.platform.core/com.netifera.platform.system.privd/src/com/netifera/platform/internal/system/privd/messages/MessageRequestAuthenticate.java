package com.netifera.platform.internal.system.privd.messages;

public class MessageRequestAuthenticate extends AbstractRequest {

	private final String password;
	
	MessageRequestAuthenticate(String password) {
		super(RequestType.PRIVD_AUTHENTICATE);
		this.password = password;
	}
	@Override
	protected void addArguments() {
		addStringArgument(password);		
	}
}
