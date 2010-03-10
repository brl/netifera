package com.netifera.platform.host.internal.terminal.ui.pty;

import java.io.IOException;
import java.io.OutputStream;

import org.eclipse.tm.internal.terminal.provisional.api.ITerminalControl;
import org.eclipse.tm.internal.terminal.provisional.api.TerminalState;
import org.eclipse.tm.internal.terminal.provisional.api.provider.TerminalConnectorImpl;

import com.netifera.platform.host.terminal.ITerminal;
import com.netifera.platform.host.terminal.ITerminalService;
import com.netifera.platform.host.terminal.ITerminalOutputHandler;

public class PTYConnector extends TerminalConnectorImpl {

	final private ITerminalService terminalManager;
	
	private ITerminal terminal;
	private OutputStream outputStream;
	
	public PTYConnector(ITerminalService terminalManager) {
		this.terminalManager = terminalManager;
	}
	
	public void connect(final ITerminalControl control) {
		super.connect(control);

		control.setState(TerminalState.CONNECTING);

		ITerminalOutputHandler outputHandler = new ITerminalOutputHandler() {
			public void terminalOutput(String ptyName, byte[] data, int length) {
				try {
					control.getRemoteToTerminalOutputStream().write(data, 0, length);
				} catch (IOException e) {
					e.printStackTrace();
				}	
			}

			public void terminalClosed(String ptyName) {
				control.displayTextInTerminal("\nTerminal Closed\n");
				disconnect();				
			}
		};

		try {
			terminal = terminalManager.openTerminal("/bin/bash", outputHandler);
		} catch (RuntimeException e) {
			e.printStackTrace();
			control.displayTextInTerminal(e.getMessage());
			control.setState(TerminalState.CLOSED);
			throw e;
		} catch (IOException e) {
			e.printStackTrace();
			control.displayTextInTerminal(e.getMessage());
			control.setState(TerminalState.CLOSED);
			throw new RuntimeException(e);
		}

		if(terminal == null) {
			control.setState(TerminalState.CLOSED);
			return;
		}
		outputStream = new PTYOutputStream(terminal);
		control.setState(TerminalState.CONNECTED);
	}
	
	@Override
	public String getSettingsSummary() {
		return null;
	}

	@Override
	public OutputStream getTerminalToRemoteStream() {
		return outputStream;
	}
	
	public void setTerminalSize(int newWidth, int newHeight) {
		terminal.setSize(newWidth, newHeight);
	}
}
