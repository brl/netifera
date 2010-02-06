package com.netifera.platform.internal.system.privd.messages;

public enum StartupType {
	PRIVD_STARTUP_OK(0),
	PRIVD_STARTUP_AUTHENTICATION_REQUIRED(1),
	PRIVD_STARTUP_NOT_ROOT(2),
	PRIVD_STARTUP_INITIALIZATION_FAILED(3),
	PRIVD_STARTUP_CONFIG_NOT_FOUND(4),
	PRIVD_STARTUP_CONFIG_BAD_PERMS(5),
	PRIVD_STARTUP_CONFIG_BAD_DATA(6);
	
	static StartupType fromCode(int code) {
		for(StartupType type : values()) {
			if(type.code == code)
				return type;
		}
		return null;
	}
	private final int code;
	
	StartupType(int code) {
		this.code = code;
	}
	int getCode() {
		return code;
	}
	

}
