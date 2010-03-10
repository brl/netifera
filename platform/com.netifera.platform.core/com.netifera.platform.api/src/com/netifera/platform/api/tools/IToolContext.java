package com.netifera.platform.api.tools;

import com.netifera.platform.api.log.ILogger;
import com.netifera.platform.api.tasks.ITaskMessenger;

public interface IToolContext extends ITaskMessenger {
	IToolConfiguration getConfiguration();
	
	long getRealm();
	long getSpaceId();
	
	void setTotalWork(int totalWork);
	void worked(int work);
	void done();
	
	void setTitle(String title);
	void setSubTitle(String subtitle);
	
	void enableDebugOutput();
	
	void debug(String message);
	void info(String message);
	void warning(String message);
	void error(String message);
	
	void print(String message);
	void exception(String message, Throwable throwable);
	
	ILogger getLogger();
}
