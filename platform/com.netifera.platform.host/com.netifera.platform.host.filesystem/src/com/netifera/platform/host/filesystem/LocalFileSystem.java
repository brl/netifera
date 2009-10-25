package com.netifera.platform.host.filesystem;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class LocalFileSystem implements IFileSystem {

	private final String rootPrefix;

	public LocalFileSystem() {
		if(System.getProperty("com.netifera.filesystemprefix") != null) {
			rootPrefix = System.getProperty("com.netifera.filesystemprefix");
		} else {
			rootPrefix = "";
		}
	}
	
	public String getNameSeparator() {
		return java.io.File.separator;
	}

	public File[] getRoots() {
		return convert(java.io.File.listRoots()); //TODO rootPrefix
	}

	public File[] getDirectoryList(String directoryName) {
		return convert((new java.io.File(rootPrefix+directoryName)).listFiles());
	}
	
	private File convert(java.io.File javaFile) {
		int attributes = 0;
		if (javaFile.isDirectory())
			attributes |= File.DIRECTORY;
		if (javaFile.isFile())
			attributes |= File.FILE;
		if (javaFile.isHidden())
			attributes |= File.HIDDEN;
		String path = javaFile.getAbsolutePath();
		if(rootPrefix.length()>0 && path.startsWith(rootPrefix)) {
			path = path.substring(rootPrefix.length());
		}
		return new File(this, path, attributes, javaFile.length(), javaFile.lastModified());
	}

	private File[] convert(java.io.File[] javaFiles) {
		if (javaFiles == null) return null;
		List<File> files = new ArrayList<File>();
		for (java.io.File javaFile: javaFiles)
			files.add(convert(javaFile));
		return files.toArray(new File[files.size()]);
	}

	public InputStream getInputStream(String fileName) throws FileNotFoundException {
		return new FileInputStream(rootPrefix+fileName);
	}

	public OutputStream getOutputStream(String fileName) throws FileNotFoundException {
		return new FileOutputStream(rootPrefix+fileName);
	}

	public boolean delete(String path) {
		java.io.File file = new java.io.File(rootPrefix+path);
		return file.delete();
	}

	public boolean deleteDirectory(String path) {
		return delete(path);
	}

	public File createDirectory(String path) {
		java.io.File file = new java.io.File(rootPrefix+path);
		file.mkdir();
		return convert(file);
	}
	
	public boolean rename(String oldName, String newName) {
		java.io.File oldFile = new java.io.File(rootPrefix+oldName);
		java.io.File newFile = new java.io.File(rootPrefix+newName);
		return oldFile.renameTo(newFile);
	}

	public String toString() {
		return "Local File System";
	}
}
