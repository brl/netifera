package com.netifera.platform.host.filesystem.spider.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.netifera.platform.api.log.ILogger;
import com.netifera.platform.host.filesystem.File;
import com.netifera.platform.host.filesystem.IFileSystem;
import com.netifera.platform.host.filesystem.spider.IFileContent;
import com.netifera.platform.host.filesystem.spider.IFileSystemSpider;
import com.netifera.platform.host.filesystem.spider.IFileSystemSpiderContext;
import com.netifera.platform.host.filesystem.spider.IFileSystemSpiderModule;
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

	public synchronized void removeModule(IFileSystemSpiderModule module) {
		modules.remove(module);
	}
	
	public synchronized List<IFileSystemSpiderModule> getModules() {
		return Collections.unmodifiableList(modules);
	}

	private synchronized void addFile(File file) {
		if (queue.size() > queueSize) {
//			logger.debug("Queue overflow, ignoring "+file);
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
			} catch (Throwable e) {
				e.printStackTrace();
				logger.error("Error starting module '"+module.getName()+"': "+e);
				logger.error("Error starting module '"+module.getName()+"'", e);
			}
		}
	}

	private synchronized void moduleHandle(IFileSystemSpiderContext context, File file, IFileContent content) {
		for (IFileSystemSpiderModule module: modules) {
			try {
				module.handle(context, file, content);
			} catch (Throwable e) {
				e.printStackTrace();
				logger.error("Error on module '"+module.getName()+"': "+e);
				logger.error("Error on module '"+module.getName()+"'", e);
			}
		}
	}

	private synchronized void moduleStop(IFileSystemSpiderContext context) {
		for (IFileSystemSpiderModule module: modules) {
			try {
				module.stop(context);
			} catch (Throwable e) {
				e.printStackTrace();
				logger.error("Error stopping module '"+module.getName()+"': "+e);
				logger.error("Error stopping module '"+module.getName()+"'", e);
			}
		}
	}

	public void run() throws InterruptedException, IOException {
		moduleStart(getContext());
		
		ExecutorService executor = Executors.newFixedThreadPool(maximumThreads);
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

			executor.shutdown();
			
			while (!executor.isTerminated()) {
				executor.awaitTermination(10000, TimeUnit.MILLISECONDS);
			}
			
			if (!executor.isTerminated())
				logger.warning("Not all threads have terminated, but exiting tool anyway");
		} finally {
			moduleStop(getContext());

			if (Thread.currentThread().isInterrupted())
				interrupted = true;
		}
	}
	
	public void fetch(String path) {
		addFile(new File(fileSystem, path));
	}
}
