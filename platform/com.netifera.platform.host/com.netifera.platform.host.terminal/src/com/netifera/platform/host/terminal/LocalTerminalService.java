package com.netifera.platform.host.terminal;

import com.netifera.platform.api.log.ILogger;
import com.netifera.platform.api.system.ISystemService;
import com.netifera.platform.host.terminal.linux.PseudoTerminal;

public class LocalTerminalService implements ITerminalService {

	private ILogger logger;
	private ISystemService system;

	public LocalTerminalService(ILogger logger, ISystemService system) {
		this.logger = logger;
		this.system = system;
	}
	
	public ITerminal openTerminal(String command, ITerminalOutputHandler outputHandler) {
		PseudoTerminal pty = new PseudoTerminal(command, outputHandler, logger, system);
		if(pty.open())
			return pty;
		return null;
	}

	public void disconnect() {
	}
}
