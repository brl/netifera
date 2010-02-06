package com.netifera.platform.internal.system.privd.messages;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

public abstract class AbstractRequest implements IMessageRequest {
	private final static int PRIVD_PROTOCOL_VERSION = 0;
	private final static int DEFAULT_REQUEST_BUFFER_SIZE = 2048;
	
	private final RequestType type;
	private final ByteBuffer requestBuffer;
	
	AbstractRequest(RequestType type) {
		this.type = type;
		requestBuffer = ByteBuffer.wrap(new byte[DEFAULT_REQUEST_BUFFER_SIZE]);
	}
	
	public byte[] getMessageBytes() throws MessageException {
		final int length = createMessage();
		byte[] messageBytes = new byte[length];
		System.arraycopy(requestBuffer.array(), 0, messageBytes, 0, length);
		return messageBytes;	
	}
	
	abstract protected void addArguments();
	
	protected void addIntegerArgument(int value) {
		requestBuffer.put(ArgumentType.PRIVD_ARG_INTEGER.getCode());
		requestBuffer.put((byte)0);
		requestBuffer.putShort((short) 4);
		requestBuffer.putInt(value);
	}
	
	protected void addStringArgument(String value) {
		int length = value.length();
		requestBuffer.put(ArgumentType.PRIVD_ARG_STRING.getCode());
		requestBuffer.put((byte)0);
		requestBuffer.putShort((short) (length + 1));
		for(char c : value.toCharArray())
			requestBuffer.put((byte)c);
		requestBuffer.put((byte)0);
		
	}
	
	private int createMessage() throws MessageException {
		try {
			return writeMessageFields();
		} catch(BufferOverflowException e) {
			throw new MessageException("Not enough space in buffer");
		}
	}
	
	private int writeMessageFields() {
		requestBuffer.clear();
		requestBuffer.put((byte) PRIVD_PROTOCOL_VERSION);
		requestBuffer.put((byte) type.getCode());
		requestBuffer.putShort((short)0);
		addArguments();
		final int length = requestBuffer.position();
		requestBuffer.putShort(2, (short)length);
		return length;
		
	}
	

}
