package com.netifera.platform.host.filesystem.tools;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import com.netifera.platform.api.probe.IProbe;
import com.netifera.platform.api.tools.ITool;
import com.netifera.platform.api.tools.IToolContext;
import com.netifera.platform.api.tools.ToolException;
import com.netifera.platform.host.filesystem.IFileSystem;
import com.netifera.platform.host.filesystem.spider.IFileSystemSpiderModule;
import com.netifera.platform.host.filesystem.spider.impl.FileSystemSpider;
import com.netifera.platform.host.filesystem.tools.internal.Activator;
import com.netifera.platform.util.addresses.inet.InternetAddress;

public class FileSystemHarvester implements ITool {
	private IToolContext context;
	
	private IFileSystem fileSystem;

	public void toolRun(IToolContext context) throws ToolException {
		this.context = context;

		context.setTitle("File System Harvester");
		setupToolOptions();

		// XXX hardcode local probe as realm
		IProbe probe = Activator.getInstance().getProbeManager().getLocalProbe();
		long realm = probe.getEntity().getId();
		
		try {
			FileSystemSpider spider = new FileSystemSpider(fileSystem);
			spider.setLogger(context.getLogger());
			spider.setRealm(realm);
			spider.setSpaceId(context.getSpaceId());
			spider.setHostAddress((InternetAddress) context.getConfiguration().get("host"));
			
			if (context.getConfiguration().get("maximumThreads") != null)
				spider.setMaximumThreads((Integer)context.getConfiguration().get("maximumThreads"));
			if (context.getConfiguration().get("bufferSize") != null)
				spider.setBufferSize((Integer)context.getConfiguration().get("bufferSize"));

			for (String moduleName: (String[]) context.getConfiguration().get("modules")) {
				addModule(spider, moduleName);
			}
				
			spider.run();
		} catch (IOException e) {
			context.exception("I/O error: " + e.getMessage(), e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			context.warning("Interrupted");
		} finally {
			context.done();
		}
	}
	
	private void setupToolOptions() throws ToolException {
		try {
			fileSystem = (IFileSystem) Activator.getInstance().getServiceFactory().create(IFileSystem.class, new URI((String)context.getConfiguration().get("target")));
			context.setTitle("Harvest "+fileSystem);
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}
	
	private void addModule(FileSystemSpider spider, String name) {
		for (IFileSystemSpiderModule module: Activator.getInstance().getFileSystemSpiderModules()) {
			if (module.getName().equals(name) || module.getClass().getName().equals(name)) {
				spider.addModule(module);
				return;
			}
		}
		context.getLogger().error("Module not found: '"+name+"'");
	}
}
