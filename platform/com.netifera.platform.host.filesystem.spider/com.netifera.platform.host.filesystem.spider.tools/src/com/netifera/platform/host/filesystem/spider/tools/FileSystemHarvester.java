package com.netifera.platform.host.filesystem.spider.tools;

import java.net.URI;
import java.net.URISyntaxException;

import com.netifera.platform.api.probe.IProbe;
import com.netifera.platform.api.tools.ITool;
import com.netifera.platform.api.tools.IToolContext;
import com.netifera.platform.api.tools.ToolException;
import com.netifera.platform.host.filesystem.IFileSystem;
import com.netifera.platform.host.filesystem.spider.impl.FileSystemSpider;
import com.netifera.platform.host.filesystem.spider.internal.tools.Activator;

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
			
			if (context.getConfiguration().get("maximumThreads") != null)
				spider.setMaximumThreads((Integer)context.getConfiguration().get("maximumThreads"));
			if (context.getConfiguration().get("bufferSize") != null)
				spider.setBufferSize((Integer)context.getConfiguration().get("bufferSize"));

			for (String moduleName: (String[]) context.getConfiguration().get("modules"))
				spider.addModule(moduleName);
				
			spider.run();
		} catch (Exception e) {
			e.printStackTrace();
/*		} catch (IOException e) {
			context.exception("I/O error: " + e.getMessage(), e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			context.warning("Interrupted");
*/		} finally {
			context.done();
		}
	}
	
	private void setupToolOptions() throws ToolException {
		try {
			fileSystem = (IFileSystem) Activator.getInstance().getServiceFactory().create(IFileSystem.class, new URI((String)context.getConfiguration().get("target")));
			context.setTitle("Harvest "+fileSystem);
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}