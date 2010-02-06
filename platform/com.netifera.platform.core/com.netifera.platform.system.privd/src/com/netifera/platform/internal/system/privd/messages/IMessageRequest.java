package com.netifera.platform.internal.system.privd.messages;

public interface IMessageRequest {
	byte[] getMessageBytes() throws MessageException;
}
