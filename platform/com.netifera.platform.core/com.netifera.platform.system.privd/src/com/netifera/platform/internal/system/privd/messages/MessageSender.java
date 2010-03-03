package com.netifera.platform.internal.system.privd.messages;

import java.nio.ByteBuffer;

import com.netifera.platform.internal.system.privd.PrivilegeDaemonNative;

public class MessageSender {
	private final static int RESPONSE_BUFFER_SIZE = 8192;
	private final static int PRIVD_PROTOCOL_VERSION = 0;
	private final static int PRIVD_HEADER_SIZE = 4;

	private final PrivilegeDaemonNative jni;
	private final ByteBuffer responseBuffer;
	
	public MessageSender() {
		jni = new PrivilegeDaemonNative();
		responseBuffer = ByteBuffer.wrap(new byte[RESPONSE_BUFFER_SIZE]);
	}
	
	public synchronized void startDaemon(String daemonPath) throws MessageException {
		if(jni.startDaemon(daemonPath) == -1)
			throw new MessageException("Could not start privilege daemon : "+ jni.getLastErrorMessage());		
	}
	
	public synchronized MessageResponseStartup readStartupMessage() throws MessageException {
		final IMessageResponse response = receiveResponse();
		if(response.getType() != ResponseType.PRIVD_RESPONSE_STARTUP || !(response instanceof MessageResponseStartup)) 
			throw new MessageException("Message received was not expected startup message type");
		return (MessageResponseStartup) response;
	}
	
	public synchronized IMessageResponse sendOpenSocket(int family, int type, int protocol) throws MessageException {
		sendRequest(new MessageRequestOpenSocket(family, type, protocol));
		return receiveResponse();
	}
	
	public synchronized IMessageResponse sendOpenBPF() throws MessageException {
		sendRequest(new MessageRequestOpenBPF());
		return receiveResponse();
	}

	public synchronized IMessageResponse sendAuthenticate(String password) throws MessageException {
		sendRequest(new MessageRequestAuthenticate(password));
		return receiveResponse();
	}

	private IMessageResponse receiveResponse() throws MessageException {
		responseBuffer.clear();
		final int length = jni.receiveMessage(responseBuffer.array());
		if(length < 0) 
			throw new MessageException(jni.getLastErrorMessage());
		
		if(responseBuffer.capacity() < length) 
			throw new MessageException("Message length exceeds buffer capacity of "+ RESPONSE_BUFFER_SIZE +" bytes.");
	
		responseBuffer.limit(length);

		return createResponseFromBuffer();
	}
	
	private void sendRequest(IMessageRequest request) throws MessageException {
		jni.sendMessage(request.getMessageBytes());
	}
	
	private IMessageResponse createResponseFromBuffer() throws MessageException {
		checkResponseHeader();	
		final ResponseType responseType = extractResponseType();
		return createMessageResponse(responseType);
	}
	
	private void checkResponseHeader() throws MessageException {
		if(responseBuffer.remaining() < PRIVD_HEADER_SIZE)
			throw new MessageException("Message length is smaller than protocol header size.  length = " + responseBuffer.remaining());
		
		final byte version = responseBuffer.get();
		if(version != PRIVD_PROTOCOL_VERSION)
			throw new MessageException("Protocol version " + PRIVD_PROTOCOL_VERSION +" expected got " + version);
	}
	
	private ResponseType extractResponseType() throws MessageException {
		final byte type = responseBuffer.get();		
		final ResponseType responseType = ResponseType.fromCode(type);
		if(responseType == null)
			throw new MessageException("Unexpected response message type " + type);
		return responseType;
	}
	
	private IMessageResponse createMessageResponse(ResponseType type) throws MessageException {
		IMessageResponse response = type.createMessage(responseBuffer);
		if(type == ResponseType.PRIVD_RESPONSE_FD) {
			final int fd = jni.getReceivedFileDescriptor();
			((MessageResponseFd)response).setReceivedFd(fd);
		}
		return response;
	}
}
