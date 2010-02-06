package com.netifera.platform.internal.system.privd.messages;

public enum ArgumentType {
	PRIVD_ARG_INTEGER(0),
	PRIVD_ARG_STRING(1);
	
	private final byte code;
	
	static ArgumentType fromCode(byte code) {
		for(ArgumentType type : values()) {
			if(type.code == code)
				return type;
		}
		return null;
	}
	ArgumentType(int code) {
		this.code = (byte)code;
	}
	byte getCode() {
		return code;
	}

}
