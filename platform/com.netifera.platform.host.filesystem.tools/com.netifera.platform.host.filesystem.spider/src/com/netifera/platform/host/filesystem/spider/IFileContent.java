package com.netifera.platform.host.filesystem.spider;

import java.io.IOException;
import java.io.InputStream;

public interface IFileContent {
	String getContentType();
	long getContentLength();
	
	InputStream getContentStream() throws IOException;
	byte[] getContent(int maxSize) throws IOException;
	byte[] getContent() throws IOException;
	
	String toString();
}
