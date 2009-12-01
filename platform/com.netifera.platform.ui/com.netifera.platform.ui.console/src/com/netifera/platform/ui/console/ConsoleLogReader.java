package com.netifera.platform.ui.console;

import com.netifera.platform.api.log.ILogEntry;
import com.netifera.platform.api.log.ILogReader;

public class ConsoleLogReader implements ILogReader {
	private final ConsoleView consoleView;
	
	ConsoleLogReader(ConsoleView view) {
		this.consoleView = view;
	}
	
	public void log(ILogEntry entry) {
		OutputState out = new OutputState(entry);
		addBanner(out);
		out.println(entry.getMessage());
		addException(out);
		printToConsole(out);
	}
	
	public void logRaw(final String message) {
		if(message.endsWith("\n"))
			consoleView.printOutput(message);	
		else
			consoleView.printOutput(message + "\n");
	}
	
	private void addBanner(OutputState out) {
		final ILogEntry entry = out.getEntry();
		
		switch(entry.getLevel()) {
		case DEBUG:
			out.print("DEBUG");
			break;
		case INFO:
			out.print("INFO");
			break;
		case WARNING:
			out.print("WARNING");
			break;
		case ERROR:
			out.print("ERROR");
			break;
		}
		
		out.print(" (" + entry.getComponent() + ") : ");
	}
	
	private void addException(OutputState out) {
		final ILogEntry entry = out.getEntry();
		if(entry.getException() != null) {
			out.printException(entry.getException());
		}
	}
	
	private void printToConsole(final OutputState out) {
		if (out.getEntry().getLevel() == ILogEntry.LogLevel.ERROR)
			consoleView.printError(out.toString());
		else
			consoleView.printOutput(out.toString());
	}
}
