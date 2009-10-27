package com.netifera.platform.host.filesystem.spider.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.netifera.platform.api.log.ILogger;
import com.netifera.platform.host.filesystem.File;
import com.netifera.platform.host.filesystem.IFileSystem;
import com.netifera.platform.host.filesystem.spider.IFileContent;
import com.netifera.platform.host.filesystem.spider.IFileSystemSpider;
import com.netifera.platform.host.filesystem.spider.IFileSystemSpiderContext;
import com.netifera.platform.host.filesystem.spider.IFileSystemSpiderModule;
import com.netifera.platform.host.internal.filesystem.spider.Activator;
import com.netifera.platform.util.BloomFilter;
import com.netifera.platform.util.addresses.inet.InternetAddress;

public class FileSystemSpider implements IFileSystemSpider {

	private volatile int bufferSize = 1024*16;
	private volatile int queueSize = 1000;
	private volatile int maximumThreads = 5;

	private InternetAddress host;
	private long realm;
	private long spaceId;

	private final IFileSystem fileSystem;

	private volatile boolean interrupted = false;

	private List<IFileSystemSpiderModule> modules = new ArrayList<IFileSystemSpiderModule>();
	
	private ILogger logger;
	
	private final Queue<File> queue = new LinkedList<File>();
	private final BloomFilter knownPaths = new BloomFilter(1024*1024); // 1M

	public FileSystemSpider(IFileSystem fileSystem) {
		this.fileSystem = fileSystem;
	}

	public void setLogger(ILogger logger) {
		this.logger = logger;
	}
	
	public void setHostAddress(InternetAddress address) {
		this.host = address;
	}
	
	public void setRealm(long realm) {
		this.realm = realm;
	}
	
	public void setSpaceId(long spaceId) {
		this.spaceId = spaceId;
	}

	public void setBufferSize(int bufferSize) {
		this.bufferSize = bufferSize;
	}

	public int getBufferSize() {
		return bufferSize;
	}

	public void setMaximumThreads(int maximumThreads) {
		this.maximumThreads = maximumThreads;
	}
	
	public int getMaximumThreads() {
		return maximumThreads;
	}

	public synchronized void addModule(IFileSystemSpiderModule module) {
		modules.add(module);
		logger.info("Enabled module: "+module.getName());
	}

	public void addModule(String name) {
		for (IFileSystemSpiderModule module: Activator.getInstance().getFileSystemSpiderModules()) {
			if (module.getName().equals(name) || module.getClass().getName().equals(name)) {
				addModule(module);
				return;
			}
		}
		logger.error("Module not found: '"+name+"'");
	}
	
	public synchronized void removeModule(IFileSystemSpiderModule module) {
		modules.remove(module);
	}
	
	public synchronized List<IFileSystemSpiderModule> getModules() {
		return Collections.unmodifiableList(modules);
	}

	private synchronized void addFile(File file) {
		if (queue.size() > queueSize) {
//			toolContext.debug("Queue overflow, ignoring "+file);
			return;
		}
		
		String path = file.getAbsolutePath();
		if (knownPaths.add(path)) // if returns true, it means it was already added to the queue before, already crawled or enqueued
			return;
		queue.add(file);
	}
	
	private synchronized boolean hasNextFile() {
		return !queue.isEmpty();
	}

	private synchronized File nextFileOrNull() {
		return queue.poll();
	}

	private IFileSystemSpiderContext getContext() {
		return new IFileSystemSpiderContext() {

			public InternetAddress getHostAddress() {
				return host;
			}

			public ILogger getLogger() {
				return logger;
			}

			public long getRealm() {
				return realm;
			}

			public long getSpaceId() {
				return spaceId;
			}

			public IFileSystemSpider getSpider() {
				return FileSystemSpider.this;
			}
		};
	}

	private IFileContent getFileContent(final File file) {
		return new IFileContent() {

			public byte[] getContent(int maxSize) throws IOException {
				// TODO Auto-generated method stub
				return null;
			}

			public byte[] getContent() throws IOException {
				// TODO Auto-generated method stub
				return null;
			}

			public long getContentLength() {
				return file.length();
			}

			public InputStream getContentStream() throws IOException {
				return file.getInputStream();
			}

			public String getContentType() {
				// TODO Auto-generated method stub
				return null;
			}
		};
	}

	private synchronized void moduleStart(IFileSystemSpiderContext context) {
		for (IFileSystemSpiderModule module: modules) {
			try {
				module.start(context);
			} catch (Exception e) {
				logger.error("Error on module "+module.getName(), e);
			}
		}
	}

	private synchronized void moduleHandle(IFileSystemSpiderContext context, File file, IFileContent content) {
		for (IFileSystemSpiderModule module: modules) {
			try {
				module.handle(context, file, content);
			} catch (Exception e) {
				logger.error("Error on module "+module.getName(), e);
			}
		}
	}

	private synchronized void moduleStop(IFileSystemSpiderContext context) {
		for (IFileSystemSpiderModule module: modules) {
			try {
				module.stop(context);
			} catch (Exception e) {
				logger.error("Error on module "+module.getName(), e);
			}
		}
	}

	public void run() throws InterruptedException, IOException {
		moduleStart(getContext());
		
		Executor executor = Executors.newFixedThreadPool(maximumThreads);
		interrupted = false;
		try {
			while (!Thread.currentThread().isInterrupted()) {
				if (!hasNextFile()) {
					logger.debug("No next File.. waiting..");
					Thread.sleep(2000);
				}
				
				if (!hasNextFile()) {
					break;
				}

				final File file = nextFileOrNull();
				if (file != null) {
					executor.execute(new Runnable() {
						public void run() {
							moduleHandle(getContext(), file, getFileContent(file));
						}
					});
				}
			}
		} finally {
			moduleStop(getContext());

			if (Thread.currentThread().isInterrupted())
				interrupted = true;
		}
	}

/*	public <A> void fetch(String path, A attachement, CompletionHandler<IFileContent,? super A> handler) {
		queue.add(new File(fileSystem, path));
	}
*/
	
	public void fetch(String path) {
		addFile(new File(fileSystem, path));
	}
}
