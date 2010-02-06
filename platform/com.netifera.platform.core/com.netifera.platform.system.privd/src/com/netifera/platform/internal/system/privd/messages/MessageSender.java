package com.netifera.platform.internal.system.privd.messages;

import java.nio.ByteBuffer;

import com.netifera.platform.internal.system.privd.PrivilegeDaemonNative;

public class MessageSender {
	private final static int PRIVD_PROTOCOL_VERSION = 0;
	private final static int PRIVD_HEADER_SIZE = 4;
	private final PrivilegeDaemonNative jni;
	private final ByteBuffer responseBuffer;
	
	public MessageSender(PrivilegeDaemonNative jni) {
		this.jni = jni;
		responseBuffer = ByteBuffer.wrap(new byte[8192]);
	}
	
	public IMessageResponse sendOpenSocket(int family, int type, int protocol) {
		return exchangeMessage(new MessageRequestOpenSocket(family, type, protocol));
	}
	
	public IMessageResponse sendOpenBPF() {
		return exchangeMessage(new MessageRequestOpenBPF());
	}
	
	
	public IMessageResponse receiveResponse() throws MessageException {
		responseBuffer.clear();
		final int length = jni.receiveMessage(responseBuffer.array());
		if(length < 0) {
			
		}
		if(responseBuffer.capacity() < length) {
			
		}
		responseBuffer.limit(length);
		return createResponseFromBuffer();
		
		
	}
	
	private IMessageResponse exchangeMessage(IMessageRequest request) {
		try {
			sendRequest(request);
			return receiveResponse();
		} catch (MessageException e) {
			e.printStackTrace();
			return null;
		}
	}
	private void sendRequest(IMessageRequest request) throws MessageException {
		jni.sendMessage(request.getMessageBytes());
	}
	
	
	private IMessageResponse createResponseFromBuffer() throws MessageException {
		if(responseBuffer.remaining() < PRIVD_HEADER_SIZE)
			throw new IllegalArgumentException("Message length is smaller than protocol header size.  length = " + responseBuffer.remaining());
		final byte version = responseBuffer.get();
		if(version != PRIVD_PROTOCOL_VERSION)
			throw new IllegalArgumentException("Protocol version " + PRIVD_PROTOCOL_VERSION + 
					" expected got " + version);
		final byte type = responseBuffer.get();
		
		final ResponseType responseType = ResponseType.fromCode(type);
		if(responseType == null)
			throw new IllegalArgumentException("Unexpected response message type " + type);

				
		IMessageResponse response = responseType.createMessage(responseBuffer);
		
		if(responseType == ResponseType.PRIVD_RESPONSE_FD) {
			final int fd = jni.getReceivedFileDescriptor();
			((MessageResponseFd)response).setReceivedFd(fd);
		}
		
		return response;
	}

}
