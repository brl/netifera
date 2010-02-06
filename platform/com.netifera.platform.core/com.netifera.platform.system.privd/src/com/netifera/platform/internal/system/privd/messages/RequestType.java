package com.netifera.platform.internal.system.privd.messages;

public enum RequestType {
	PRIVD_PING(0),
	PRIVD_AUTHENTICATE(1),
	PRIVD_OPEN_SOCKET(2),
	PRIVD_OPEN_BPF(3);
	
	private final byte code;
	
	RequestType(int code) {
		this.code = (byte) code;
	}
	byte getCode() {
		return code;
	}

}
