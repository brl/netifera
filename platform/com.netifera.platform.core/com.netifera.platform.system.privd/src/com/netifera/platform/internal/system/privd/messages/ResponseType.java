package com.netifera.platform.internal.system.privd.messages;

import java.nio.ByteBuffer;

public enum ResponseType {
	PRIVD_RESPONSE_OK(0) {
		AbstractResponse createMessage(ByteBuffer buffer) throws MessageException {
			return new MessageResponseOk(buffer);
		}
	},
	PRIVD_RESPONSE_ERROR(1) {
		AbstractResponse createMessage(ByteBuffer buffer) throws MessageException {
			return new MessageResponseError(buffer);
		}
	},
	PRIVD_RESPONSE_STARTUP(2) {
		AbstractResponse createMessage(ByteBuffer buffer) throws MessageException {
			return new MessageResponseStartup(buffer);
		}
	},
	PRIVD_RESPONSE_FD(3) {
		AbstractResponse createMessage(ByteBuffer buffer) throws MessageException {
			return new MessageResponseFd(buffer);
		}
	},
	PRIVD_RESPONSE_AUTH_FAILED(4) {
		public AbstractResponse createMessage(ByteBuffer buffer)
				throws MessageException {
			return new MessageResponseAuthFailed(buffer);
		}
	};
	
	abstract AbstractResponse createMessage(ByteBuffer buffer) throws MessageException;
	private final byte code;
	static ResponseType fromCode(byte code) {
		for(ResponseType type : values()) {
			if(type.code == code)
				return type;
		}
		return null;
	}
	ResponseType(int code) {
		this.code = (byte) code;
	}
	byte getCode() {
		return code;
	}

}
