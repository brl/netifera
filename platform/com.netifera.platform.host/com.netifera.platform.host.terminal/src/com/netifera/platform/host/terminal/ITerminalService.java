package com.netifera.platform.host.terminal;

import java.io.IOException;

public interface ITerminalService {
	ITerminal openTerminal(String command, ITerminalOutputHandler outputHandler) throws IOException;
	void disconnect();
}
