package com.netifera.platform.net.cifs.filesystem;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

import com.netifera.platform.host.filesystem.File;
import com.netifera.platform.host.filesystem.IFileSystem;

public class SMBFileSystem implements IFileSystem {
	private SmbFile share;
	
	public SMBFileSystem(String url) throws MalformedURLException {
		share = new SmbFile(url);
	}

	public SMBFileSystem(URI url) {
		try {
			share = new SmbFile(url.toASCIIString());
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}

	public String getNameSeparator() {
		return "/";
	}

	public File createDirectory(String directoryName) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean delete(String fileName) throws IOException {
		(new SmbFile(fileName)).delete();
		return true;
	}

	public boolean deleteDirectory(String directoryName) throws IOException {
		return delete(directoryName);
	}

	public File[] getDirectoryList(String directoryName) throws IOException {
		return convert((new SmbFile(directoryName)).listFiles());
	}

	public InputStream getInputStream(String fileName) throws IOException {
		return (new SmbFile(fileName)).getInputStream();
	}
	
	public OutputStream getOutputStream(String fileName) throws IOException {
		return (new SmbFile(fileName)).getOutputStream();
	}

	public File[] getRoots() {
		try {
			return convert(new SmbFile[] {share});
		} catch (SmbException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public boolean rename(String oldName, String newName) throws IOException {
		(new SmbFile(oldName)).renameTo(new SmbFile(newName));
		return true;
	}

	public void disconnect() throws IOException {
		// TODO Auto-generated method stub
	}

	private File[] convert(SmbFile[] smbFiles) throws SmbException {
		List<File> files = new ArrayList<File>();
		for (SmbFile smbFile: smbFiles) {
			files.add(convert(smbFile));
		}
		return files.toArray(new File[files.size()]);
	}
	
	private File convert(SmbFile smbFile) throws SmbException {
		int attributes = 0;
		if (smbFile.isDirectory())
			attributes |= File.S_IFDIR;
		if (smbFile.isFile())
			attributes |= File.S_IFREG;
		String fullPath = smbFile.getCanonicalPath();
//		if (fullPath.endsWith("/"))
//			fullPath = fullPath.substring(0, fullPath.length()-1);
		return new File(this, fullPath, attributes, smbFile.length(), smbFile.getLastModified());
	}
	
	public String toString() {
		return share.toString();
	}
}
