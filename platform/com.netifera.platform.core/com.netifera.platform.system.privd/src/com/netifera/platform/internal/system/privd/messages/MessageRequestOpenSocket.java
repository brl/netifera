package com.netifera.platform.internal.system.privd.messages;

public class MessageRequestOpenSocket extends AbstractRequest{

	private final int family;
	private final int type;
	private final int protocol;
	
	MessageRequestOpenSocket(int family, int type, int protocol) {
		super(RequestType.PRIVD_OPEN_SOCKET);
		this.family = family;
		this.type = type;
		this.protocol = protocol;
	}
	@Override
	protected void addArguments() {
		addIntegerArgument(family);
		addIntegerArgument(type);
		addIntegerArgument(protocol);		
	}

}
