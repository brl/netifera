package com.netifera.platform.host.filesystem;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;


public class File implements Serializable {
	private static final long serialVersionUID = -5668415766862514374L;
	
	/**
	 * POSIX permissions
	 */
	public static final int S_IFMT = 0170000;	// bitmask for the file type bitfields
	public static final int S_IFSOCK = 0140000;	// socket
	public static final int S_IFLNK = 0120000;	// symbolic link
	public static final int S_IFREG = 0100000;	// regular file
	public static final int S_IFBLK = 0060000;	// block device
	public static final int S_IFDIR = 0040000;	// directory
	public static final int S_IFCHR = 0020000;	// character device
	public static final int S_IFIFO = 0010000;	// fifo 
	public static final int S_ISUID = 0004000;	// set UID bit
	public static final int S_ISGID = 0002000;	// set GID bit 
	public static final int S_ISVTX = 0001000;	// sticky bit
	
	public static final int S_IRWXU = 00700;	// mask for file owner permissions
	public static final int S_IRUSR = 00400;	// owner has read permission
	public static final int S_IWUSR = 00200;	// owner has write permission
	public static final int S_IXUSR = 00100;	// owner has execute permission
	public static final int S_IRWXG = 00070;	// mask for group permissions
	public static final int S_IRGRP = 00040;	// group has read permission
	public static final int S_IWGRP = 00020;	// group has write permission
	public static final int S_IXGRP = 00010;	// group has execute permission
	public static final int S_IRWXO = 00007;	// mask for permissions for others (not in group)
	public static final int S_IROTH = 00004;	// others have read permission
	public static final int S_IWOTH = 00002;	// others have write permission
	public static final int S_IXOTH = 00001;	// others have execute permission

/*	public static final int DIRECTORY = 1;
	public static final int FILE = 2;
	public static final int SYMBOLIC_LINK = 4;
	public static final int HIDDEN = 8;
*/	
	transient private IFileSystem fileSystem;
	private String path;
	
	
	final private int permissions;
	
	final private long length;
	final private long lastModified;

	public File(IFileSystem fileSystem, String path) {
		this(fileSystem, path, path.endsWith(fileSystem.getNameSeparator()) ? File.S_IFDIR : File.S_IFREG, 0, 0);
	}

	public File(IFileSystem fileSystem, String path, int permissions, long length, long lastModified) {
		this.fileSystem = fileSystem;
		this.path = path;
		this.permissions = permissions;
		this.length = length;
		this.lastModified = lastModified;
	}
	
	public void setFileSystem(IFileSystem fileSystem) {
		this.fileSystem = fileSystem;
	}
	
	public IFileSystem getFileSystem() {
		return fileSystem;
	}
	
	public String getName() {
		String path = this.path;
		if (path.endsWith(fileSystem.getNameSeparator()))
			path = path.substring(0, path.length()-1);
		return path.substring(path.lastIndexOf(fileSystem.getNameSeparator())+1);
	}
	
	public String getAbsolutePath() {
		return path;
	}
	
	public boolean isDirectory() {
		return (permissions & File.S_IFDIR) != 0;
	}

	public boolean isFile() {
		return (permissions & File.S_IFREG) != 0;
	}
	
	public boolean isSymbolicLink() {
		return (permissions & File.S_IFLNK) != 0;
	}

	public long length() {
		return length;
	}
	
	public long lastModified() {
		return lastModified;
	}
	
	public File getParent() {
		int lastIndex = path.lastIndexOf(fileSystem.getNameSeparator());
		if (lastIndex <= 0)
			return null;
		return new File(fileSystem, path.substring(0, lastIndex), File.S_IFDIR, 0, 0);
	}
	
	public boolean delete() throws IOException {
		if (isDirectory()) {
			return fileSystem.deleteDirectory(path);
		} else {
			return fileSystem.delete(path);
		}
	}
	
	public boolean renameTo(String newName) throws IOException {
		if (fileSystem.rename(getAbsolutePath(), newName)) {
			path = newName;
			return true;
		}
		return false;
	}

	public InputStream getInputStream() throws IOException {
		return fileSystem.getInputStream(path);
	}

	public OutputStream getOutputStream() throws IOException {
		return fileSystem.getOutputStream(path);
	}

	public boolean equals(Object o) {
		if (!(o instanceof File))
			return false;
		File file = (File) o;
		return fileSystem == file.fileSystem && (path.equals(file.path));
	}
	
	public int hashCode() {
		return path.hashCode();
	}
	
	public String toString() {
		return path;
	}
}
