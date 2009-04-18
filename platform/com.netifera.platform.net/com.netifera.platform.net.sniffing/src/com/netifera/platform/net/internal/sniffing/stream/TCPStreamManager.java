package com.netifera.platform.net.internal.sniffing.stream;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import com.netifera.platform.net.sniffing.stream.ISessionKey;
import com.netifera.platform.net.sniffing.stream.IStreamSniffer;
import com.netifera.platform.net.sniffing.stream.IStreamSnifferHandle;

public class TCPStreamManager {
	private final ISessionKey key;
	private final Object sessionTag;
	private final Collection<IStreamSnifferHandle> streamHandles;
	private boolean closed = false;
	TCPStreamManager(ISessionKey key, Object sessionTag, Collection<IStreamSnifferHandle> handles) {
		this.streamHandles = new HashSet<IStreamSnifferHandle>(handles);
		this.key = key;
		this.sessionTag = sessionTag;
	}
	
	synchronized void unregisterHandle(IStreamSnifferHandle handle) {
		streamHandles.remove(handle);
	}
	
	synchronized void handleEstablished(IStreamSniffer.SessionType sessionType) {
		for(Iterator<IStreamSnifferHandle> itr = streamHandles.iterator(); itr.hasNext(); ) {
			IStreamSnifferHandle handle = itr.next();
			if(handle.getSniffer().handleNewSession(new TCPSessionContext(key, getTagForHandle(handle)), sessionType) == false) {
				itr.remove();
			}
		}
	}
	
	synchronized void handleClientData(ByteBuffer data) {
		for(Iterator<IStreamSnifferHandle> itr = streamHandles.iterator(); itr.hasNext(); ) {
			IStreamSnifferHandle handle = itr.next();
			if(handle.getSniffer().handleClientData(new TCPSessionContext(key, getTagForHandle(handle)), data) == false) {
				itr.remove();
			}
		}
	}
	
	synchronized void handleServerData(ByteBuffer data) {
		for(Iterator<IStreamSnifferHandle> itr = streamHandles.iterator(); itr.hasNext(); ) {
			IStreamSnifferHandle handle = itr.next();
			if(handle.getSniffer().handleServerData(new TCPSessionContext(key, getTagForHandle(handle)), data) == false) {
				itr.remove();
			}
		}
	}
	
	synchronized void handleClose() {
		closed = true;
		for(IStreamSnifferHandle handle : streamHandles) {
			handle.getSniffer().handleSessionClose(new TCPSessionContext(key, getTagForHandle(handle)));
		}
	}
	
	private Object getTagForHandle(IStreamSnifferHandle handle) {
		if(sessionTag != null)
			return sessionTag;
		else
			return handle.getDefaultTag();
	}
	boolean isActive() {
		return streamHandles.size() > 0;
	}
	
	void shutdown() {
		if(!closed) {
			handleClose();
		}
		streamHandles.clear();
	}
}
