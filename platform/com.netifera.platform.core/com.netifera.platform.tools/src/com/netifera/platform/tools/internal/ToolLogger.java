package com.netifera.platform.tools.internal;

import com.netifera.platform.api.log.ILogManager;
import com.netifera.platform.api.log.ILogger;
import com.netifera.platform.api.tools.IToolContext;

public class ToolLogger implements ILogger {
	
	private final IToolContext context;
	
	public ToolLogger(IToolContext context) {
		this.context = context;
	}
	
	public void debug(String message) {
		context.debug(message);
	}

	public void debug(String message, Throwable exception) {
		context.exception(message, exception);
	}

	public void disableDebug() {
	}

	public void enableDebug() {
		context.enableDebugOutput();
	}

	public void error(String message) {
		context.error(message);
	}

	public void error(String message, Throwable exception) {
		context.exception(message, exception);
	}

	public ILogManager getManager() {
		return null;
	}

	public void info(String message) {
		context.info(message);
	}

	public void info(String message, Throwable exception) {
		context.exception(message, exception);
	}

	public void warning(String message) {
		context.warning(message);
	}

	public void warning(String message, Throwable exception) {
		context.exception(message, exception);
	}
}
