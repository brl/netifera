package com.netifera.platform.api.tools;

public interface ITool {
	void run(IToolContext context) throws ToolException;
}
