package com.netifera.platform.net.http.spider.impl;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;

import com.netifera.platform.net.http.spider.HTTPResponse;

public class HTTPResponseAdapter implements HTTPResponse {
	
	private final HttpResponse response;
	private final int bufferSize;
	private final HttpEntity entity;
	private final BufferedInputStream contentStream;
	
	public HTTPResponseAdapter(HttpResponse response, int bufferSize) throws IOException {
		this.response = response;
		this.bufferSize = bufferSize;
		entity = response.getEntity();
		try {
			entity.consumeContent();
		} catch (Exception e) {
			e.printStackTrace();
		}
		contentStream = new BufferedInputStream(entity.getContent(), bufferSize);
	}

	public int getStatusCode() {
		return response.getStatusLine().getStatusCode();
	}

	public String getHeader(String headerName) {
		Header header = response.getFirstHeader(headerName);
		return header == null ? null : header.getValue();
	}

	public long getContentLength() {
		return entity.getContentLength();
	}

	public String getContentType() {
		return entity.getContentType().getValue();
	}

	public InputStream getContentStream() throws IOException {
		return contentStream;
	}

	public byte[] getContent(int maxSize) throws IOException {
		if (maxSize > bufferSize) {
			throw new RuntimeException("Attempting to peek ahead of peek buffer size");
		}
		long contentLength = getContentLength();
		if (contentLength > 0 && contentLength < maxSize)
			maxSize = (int)contentLength;
		byte[] content = new byte[maxSize];
		contentStream.mark(maxSize);
		int count = 0;
		try {
			count = contentStream.read(content);
		} finally {
			contentStream.reset();
		}
		return Arrays.copyOf(content, count);
	}
	
	public byte[] getContent() throws IOException {
		return getContent((int)getContentLength());
	}

	public String toString() {
		StringBuffer buffer = new StringBuffer();
		
		buffer.append(response.getStatusLine());
		buffer.append('\n');
		for (Header header: response.getAllHeaders()) {
			buffer.append(header);
			buffer.append('\n');
		}
		buffer.append('\n');

		try {
			buffer.append(new String(getContent(bufferSize)));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return buffer.toString();
	}
}
