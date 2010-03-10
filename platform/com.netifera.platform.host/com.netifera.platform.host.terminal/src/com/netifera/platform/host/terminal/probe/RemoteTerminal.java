package com.netifera.platform.host.terminal.probe;

import com.netifera.platform.api.dispatcher.IMessenger;
import com.netifera.platform.api.dispatcher.IProbeMessage;
import com.netifera.platform.api.dispatcher.MessengerException;
import com.netifera.platform.host.terminal.ITerminal;
import com.netifera.platform.host.terminal.ITerminalOutputHandler;

public class RemoteTerminal implements ITerminal {

	private final String ptyName;
	private final RemoteTerminalService service;
	private final ITerminalOutputHandler outputHandler;
	private int lastWidth = -1;
	private int lastHeight = -1;
	
	RemoteTerminal(String ptyName, RemoteTerminalService service, ITerminalOutputHandler outputHandler) {
		this.ptyName = ptyName;
		this.service = service;
		this.outputHandler = outputHandler;
	}
	
	public String getName() {
		return ptyName;
	}

	public void sendInput(byte[] data) {
		emitMessage(new TerminalInput(ptyName, data));
	}
	
	public void receiveOutput(byte[] data) {
		outputHandler.terminalOutput(ptyName, data, data.length);		
	}
	
	public void setSize(int width, int height) {
		
		if(width == lastWidth && height == lastHeight)
			return;
		
		emitMessage(new TerminalSizeChange(ptyName, width, height));
		
		lastWidth = width;
		lastHeight = height;
		
	}
	
	public void close() {
		emitMessage(new CloseTerminal(ptyName));
	}
	
	void setClosed() {
		service.removeTerminal(ptyName);
		outputHandler.terminalClosed(ptyName);
	}
	
	private void emitMessage(IProbeMessage message) {
		final IMessenger messenger = service.getProbe().getMessenger();
		if(messenger == null) {
			service.getLogger().error("Cannot send message because probe has no active messenger");
			setClosed();
			return;
		}
		
		try {
			messenger.emitMessage(message);
		} catch (MessengerException e) {
			service.getLogger().warning("Messenger error sending message " + e.getMessage(), e);
			setClosed();
		}
	}
}
