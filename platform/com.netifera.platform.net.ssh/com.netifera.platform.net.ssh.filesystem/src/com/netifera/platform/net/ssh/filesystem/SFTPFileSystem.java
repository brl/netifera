package com.netifera.platform.net.ssh.filesystem;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import com.netifera.platform.host.filesystem.File;
import com.netifera.platform.host.filesystem.IFileSystem;
import com.netifera.platform.net.services.credentials.Credential;
import com.netifera.platform.net.services.credentials.UsernameAndPassword;
import com.netifera.platform.net.services.ssh.SSH;
import com.netifera.platform.util.addresses.inet.InternetAddress;
import com.netifera.platform.util.addresses.inet.TCPSocketAddress;
import com.trilead.ssh2.Connection;
import com.trilead.ssh2.SFTPException;
import com.trilead.ssh2.SFTPv3Client;
import com.trilead.ssh2.SFTPv3DirectoryEntry;
import com.trilead.ssh2.SFTPv3FileAttributes;
import com.trilead.ssh2.SFTPv3FileHandle;
import com.trilead.ssh2.sftp.ErrorCodes;

public class SFTPFileSystem implements IFileSystem {

	private SSH ssh;
	private Credential credential;

	public SFTPFileSystem(URI url) {
		InternetAddress address = InternetAddress.fromString(url.getHost());
		TCPSocketAddress socketAddress = new TCPSocketAddress(address, url.getPort());
		this.ssh = new SSH(socketAddress);

		String[] userInfo = url.getUserInfo().split(":");
		String username = userInfo[0];
		String password = userInfo.length > 1 ? userInfo[1] : "";
		this.credential = new UsernameAndPassword(username, password);
	}
	
	public SFTPFileSystem(SSH ssh, Credential credential) {
		this.ssh = ssh;
		this.credential = credential;
	}

	public String getNameSeparator() {
		return "/";
	}
	
	public File[] getRoots() {
		return new File[] {new File(this, "/", File.S_IFDIR, 0, 0)};
	}

	@SuppressWarnings("unchecked")
	public File[] getDirectoryList(String directoryName) throws IOException {
		SFTPv3Client client = new SFTPv3Client(ssh.createConnection(credential));
		try {
			return convert(directoryName, client.ls(directoryName));
		} catch (SFTPException e) {
			if (e.getServerErrorCode() == ErrorCodes.SSH_FX_NO_SUCH_FILE)
				throw new FileNotFoundException(directoryName);
			throw e;
		} finally {
			client.close();
		}
	}

	public File createDirectory(String directoryName) throws IOException {
		Connection connection = ssh.createConnection(credential);
		SFTPv3Client client = new SFTPv3Client(connection);
		try {
			client.mkdir(directoryName, 0775);
			return new File(this, directoryName, File.S_IFDIR, 0, 0);
		} finally {
			client.close();
			connection.close();
		}
	}

	public boolean delete(String fileName) throws IOException {
		Connection connection = ssh.createConnection(credential);
		SFTPv3Client client = new SFTPv3Client(connection);
		try {
			client.rm(fileName);
			return true;
		} finally {
			client.close();
			connection.close();
		}
//		return false;
	}

	public boolean deleteDirectory(String directoryName) throws IOException {
		Connection connection = ssh.createConnection(credential);
		SFTPv3Client client = new SFTPv3Client(connection);
		try {
			client.rmdir(directoryName);
			return true;
		} catch (SFTPException e) {
			if (e.getServerErrorCode() == ErrorCodes.SSH_FX_NO_SUCH_FILE)
				throw new FileNotFoundException(directoryName);
			throw e;
		} finally {
			client.close();
			connection.close();
		}
//		return false;
	}


	public File stat(String fileName) throws IOException {
		Connection connection = ssh.createConnection(credential);
		SFTPv3Client client = new SFTPv3Client(connection);
		try {
			SFTPv3FileAttributes attributes = client.stat(fileName);
			return new File(this, fileName, attributes.permissions, attributes.size, attributes.mtime);
		} catch (SFTPException e) {
			if (e.getServerErrorCode() == ErrorCodes.SSH_FX_NO_SUCH_FILE)
				throw new FileNotFoundException(fileName);
			throw e;
		} finally {
			client.close();
			connection.close();
		}
	}

	public boolean rename(String oldName, String newName) throws IOException {
		Connection connection = ssh.createConnection(credential);
		SFTPv3Client client = new SFTPv3Client(connection);
		try {
			client.mv(oldName, newName);
			return true;
		} catch (SFTPException e) {
			if (e.getServerErrorCode() == ErrorCodes.SSH_FX_NO_SUCH_FILE)
				throw new FileNotFoundException(oldName);
			throw e;
		} finally {
			client.close();
			connection.close();
		}
//		return false;
	}

	private File convert(String directoryPath, SFTPv3DirectoryEntry sftpFile) {
		int attributes = 0;
		if (sftpFile.attributes.isDirectory())
			attributes |= File.S_IFDIR;
		if (sftpFile.attributes.isRegularFile())
			attributes |= File.S_IFREG;
		if (sftpFile.attributes.isSymlink())
			attributes |= File.S_IFLNK;
		if (!directoryPath.endsWith("/"))
			directoryPath += "/";
		String fullPath = directoryPath+sftpFile.filename;
		if (fullPath.endsWith("/"))
			fullPath = fullPath.substring(0, fullPath.length()-1);
		return new File(this, fullPath, attributes, sftpFile.attributes.size, sftpFile.attributes.mtime);
	}

	private File[] convert(String directoryPath, Vector<SFTPv3DirectoryEntry> sftpFiles) {
		List<File> files = new ArrayList<File>();
		for (SFTPv3DirectoryEntry sftpFile: sftpFiles)
			if (!sftpFile.filename.equals("..") && !sftpFile.filename.equals("."))
				files.add(convert(directoryPath, sftpFile));
		return files.toArray(new File[files.size()]);
	}

	public InputStream getInputStream(String fileName) throws IOException {
		try {
			final Connection connection = ssh.createConnection(credential);
			SFTPv3Client client = new SFTPv3Client(connection);
			SFTPv3FileAttributes attributes = client.stat(fileName);
			SFTPv3FileHandle handle = client.openFileRO(fileName);
			return new SFTPInputStream(connection, client, handle, attributes.size);
		} catch (SFTPException e) {
			if (e.getServerErrorCode() == ErrorCodes.SSH_FX_NO_SUCH_FILE)
				throw new FileNotFoundException(fileName);
			throw e;
		}
	}

	public OutputStream getOutputStream(String fileName) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	public void disconnect() throws IOException {
		// TODO Auto-generated method stub
	}

	@Override
	public String toString() {
		return ssh.toString();
	}
}
