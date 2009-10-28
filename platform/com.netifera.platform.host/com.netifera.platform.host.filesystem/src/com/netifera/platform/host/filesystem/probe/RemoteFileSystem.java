package com.netifera.platform.host.filesystem.probe;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

import com.netifera.platform.api.dispatcher.IProbeMessage;
import com.netifera.platform.api.dispatcher.MessengerException;
import com.netifera.platform.api.log.ILogger;
import com.netifera.platform.api.probe.IProbe;
import com.netifera.platform.dispatcher.StatusMessage;
import com.netifera.platform.host.filesystem.File;
import com.netifera.platform.host.filesystem.IFileSystem;

public class RemoteFileSystem implements IFileSystem {

	private final IProbe probe;
	private final ILogger logger;
	private final URI url;
	private String messengerError;
	
	public RemoteFileSystem(URI url, IProbe probe, ILogger logger) {
		this.url = url;
		this.probe = probe;
		this.logger = logger;
	}
	
	public File createDirectory(String directoryName) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean delete(String fileName) throws IOException {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean deleteDirectory(String directoryName) throws IOException {
		// TODO Auto-generated method stub
		return false;
	}

	public File[] getDirectoryList(String directoryName) throws IOException {
		final GetDirectoryListing msg = (GetDirectoryListing) exchangeMessage(new GetDirectoryListing(url, directoryName));
		if(msg == null) {
			logger.warning("GetDirectoryList failed " + messengerError);
			return null;
		}
		for(File f : msg.getDirectoryEntries())
			f.setFileSystem(this);
		
		return msg.getDirectoryEntries();
	}

	public InputStream getInputStream(String fileName) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	public String getNameSeparator() {
		return java.io.File.separator;
	}

	public OutputStream getOutputStream(String fileName) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	public File[] getRoots() {
		final GetRoots msg = (GetRoots) exchangeMessage(new GetRoots(url));
		if(msg == null) {
			logger.warning("GetRoots failed" + messengerError);
			return null;
		}
		for(File f : msg.getFileRoots()) {
			f.setFileSystem(this);
		}
		return msg.getFileRoots();
	}

	public boolean rename(String oldName, String newName) throws IOException {
		// TODO Auto-generated method stub
		return false;
	}
	
	public void disconnect() throws IOException {
		// TODO Auto-generated method stub
	}
	
	@SuppressWarnings("unused")
	private boolean sendMessage(IProbeMessage message) {
		try {
			probe.getMessenger().sendMessage(message);
			return true;
		} catch (MessengerException e) {
			messengerError = e.getMessage();
			return false;
		}
	}
	
	private IProbeMessage exchangeMessage(IProbeMessage message) {
		try {
			IProbeMessage response = probe.getMessenger().exchangeMessage(message);
			if(response instanceof StatusMessage) { 
				return null;
			} else {
				return response;
			}
		} catch (MessengerException e) {
			messengerError = e.getMessage();
			return null;
		}
	}
	
	public String toString() {
		if (url.getScheme().equals("local"))
			return probe.getName();
		return url.toString() + " on "+probe.getName();
	}
}
