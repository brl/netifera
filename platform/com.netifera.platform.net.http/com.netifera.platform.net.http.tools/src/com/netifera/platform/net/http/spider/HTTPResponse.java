package com.netifera.platform.net.http.spider;

import java.io.IOException;
import java.io.InputStream;

public interface HTTPResponse {
	
	int getStatusCode();
	String getHeader(String headerName);
	
	String getContentType();
	long getContentLength();
	
	InputStream getContentStream() throws IOException;
	byte[] getContent(int maxSize) throws IOException;
	byte[] getContent() throws IOException;
	
	String toString();
//	byte[] getContentBytes(int maxSize);
}
