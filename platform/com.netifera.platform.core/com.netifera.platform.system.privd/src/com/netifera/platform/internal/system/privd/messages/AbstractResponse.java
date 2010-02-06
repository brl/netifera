package com.netifera.platform.internal.system.privd.messages;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

abstract public class AbstractResponse implements IMessageResponse {
	
	private ResponseType responseType;
	private ByteBuffer messageBuffer;
	
	
	AbstractResponse(ResponseType type, ByteBuffer buffer) throws MessageException {
		this.responseType = type;
		this.messageBuffer = buffer;
		parseMessage();
	}
	
	public ResponseType getType() {
		return responseType;
	}
	
	abstract protected void parseArguments();
	
	protected boolean hasAnotherArgument() {
		return messageBuffer.remaining() > 4;
	}
	protected int parseIntegerArgument() {
		final byte type = messageBuffer.get();
		if(type != ArgumentType.PRIVD_ARG_INTEGER.getCode()) {
			
		}
		messageBuffer.get();
		final int argumentLength = messageBuffer.getShort() & 0xFFFF;
		if(argumentLength != 4) {
			
		}
		return messageBuffer.getInt();
		
	}
	
	protected String parseStringArgument() {
		final byte type = messageBuffer.get();
		if(type != ArgumentType.PRIVD_ARG_STRING.getCode()) {
			
		}
		messageBuffer.get();
		final int argumentLength = messageBuffer.getShort() & 0xFFFF;
		final byte[] stringBytes = new byte[argumentLength];
		messageBuffer.get(stringBytes);
		return new String(stringBytes);
	}
	
	private void parseMessage() throws MessageException {
		final int length = messageBuffer.getShort() & 0xFFFF;
		
		if(length != messageBuffer.limit()) 
			throw new IllegalArgumentException("Message length field does not match size of received message: " +
					length + " vs. " + messageBuffer.limit());
		
		try {
			parseArguments();
		} catch(BufferUnderflowException e) {
			throw new MessageException("Ran out of space parsing response message.");
		}
		
	}

}
