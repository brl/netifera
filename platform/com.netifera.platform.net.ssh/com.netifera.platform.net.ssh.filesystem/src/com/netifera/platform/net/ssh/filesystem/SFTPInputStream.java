package com.netifera.platform.net.ssh.filesystem;

import java.io.IOException;
import java.io.InputStream;

import com.trilead.ssh2.Connection;
import com.trilead.ssh2.SFTPv3Client;
import com.trilead.ssh2.SFTPv3FileHandle;

public class SFTPInputStream extends InputStream {
	private final Connection connection;
	private final SFTPv3Client client;
	private final SFTPv3FileHandle handle;
	private long fileOffset = 0;
	private long totalSize = 0;
	
	public SFTPInputStream(Connection connection, SFTPv3Client client, SFTPv3FileHandle handle, long totalSize) throws IOException {
		this.connection = connection;
		this.client = client;
		this.handle = handle;
		this.totalSize = totalSize;
	}

	@Override
	public int available() {
		return (int) Math.max(0, totalSize - fileOffset);
	}

    public long skip(long n) {
    	long actuallySkipped = Math.min(n, available());
    	fileOffset = fileOffset + actuallySkipped;
    	return actuallySkipped;
    }
    
	@Override
	public int read() throws IOException {
		byte[] dst = new byte[1];
		if (client.read(handle, fileOffset, dst, 0, 1) == -1)
			return -1;
		fileOffset = fileOffset + 1;
		return dst[0] & 0xFF;
	}

    public int read(byte b[], int off, int len) throws IOException {
    	if (b == null) {
    	    throw new NullPointerException();
    	} else if (off < 0 || len < 0 || len > b.length - off) {
    	    throw new IndexOutOfBoundsException();
    	} else if (len == 0) {
    	    return 0;
    	}
    	
    	int count = client.read(handle, fileOffset, b, off, len);
    	if (count > 0)
    		fileOffset = fileOffset + count;
    	return count;
    }
    
	@Override
	public void close() throws IOException {
		try {
			client.closeFile(handle);
		} finally {
			client.close();
			connection.close();
		}
	}
}
