package com.netifera.platform.net.http.tools;

import java.net.URI;
import java.net.URISyntaxException;

import com.netifera.platform.api.probe.IProbe;
import com.netifera.platform.api.tools.ITool;
import com.netifera.platform.api.tools.IToolContext;
import com.netifera.platform.api.tools.ToolException;
import com.netifera.platform.net.http.internal.tools.Activator;
import com.netifera.platform.net.http.service.HTTP;
import com.netifera.platform.net.http.spider.impl.WebSpider;
import com.netifera.platform.net.http.spider.modules.CrawlBackupFilesModule;
import com.netifera.platform.net.http.spider.modules.CrawlDefaultFilesModule;
import com.netifera.platform.net.http.spider.modules.EmailsHarvesterModule;
import com.netifera.platform.net.http.spider.modules.FaviconHarvesterModule;
import com.netifera.platform.net.http.spider.modules.HTTPBasicAuthExtractorModule;
import com.netifera.platform.net.http.spider.modules.WebApplicationDetectorModule;
import com.netifera.platform.tools.RequiredOptionMissingException;

public class WebCrawler implements ITool {
	private IToolContext context;
	private HTTP http;
	private URI base;

	public void toolRun(IToolContext context) throws ToolException {
		this.context = context;

		context.setTitle("Web crawler");
		setupToolOptions();

		// XXX hardcode local probe as realm
		IProbe probe = Activator.getInstance().getProbeManager().getLocalProbe();
		long realm = probe.getEntity().getId();
		
		String host = base.getHost();
		if (host != null && host.compareTo(http.getURIHost()) == 0) {
			context.setTitle("Crawl "+base);
		} else {
			context.setTitle("Crawl "+base+" at "+http.getLocator());
		}
		
		try {
			WebSpider spider = new WebSpider();
			spider.setServices(context.getLogger(), Activator.getInstance().getWebEntityFactory(), Activator.getInstance().getNameResolver());
			spider.setRealm(realm);
			spider.setSpaceId(context.getSpaceId());
			spider.addTarget(http, base.getHost());
			spider.visit(base);
			
			spider.addModule(new FaviconHarvesterModule());
			spider.addModule(new EmailsHarvesterModule());
			spider.addModule(new HTTPBasicAuthExtractorModule());
			spider.addModule(new CrawlBackupFilesModule());
			spider.addModule(new CrawlDefaultFilesModule());
			
			if (context.getConfiguration().get("followLinks") != null)
				spider.setFollowLinks((Boolean)context.getConfiguration().get("followLinks"));
			if (context.getConfiguration().get("fetchImages") != null)
				spider.setFetchImages((Boolean)context.getConfiguration().get("fetchImages"));
			if (context.getConfiguration().get("scanWebApplications") != null)
				if ((Boolean)context.getConfiguration().get("scanWebApplications"))
					spider.addModule(new WebApplicationDetectorModule());
			if (context.getConfiguration().get("maximumConnections") != null)
				spider.setMaximumConnections((Integer)context.getConfiguration().get("maximumConnections"));
			if (context.getConfiguration().get("bufferSize") != null)
				spider.setBufferSize((Integer)context.getConfiguration().get("bufferSize"));

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
		http = (HTTP) context.getConfiguration().get("target");
		
		String url = (String) context.getConfiguration().get("url");
		if (url == null)
			throw new RequiredOptionMissingException("url");
		if (url.length() == 0)
			throw new ToolException("Empty URL parameter");

		// if no port
		if (url.startsWith("/")) {
			url = http.getURIHostPort() + url;
		}
		
		if (!url.contains("/")) {
			url += '/';
		}
		
		// if no protocol
		if (!url.startsWith("http")) {
			//url = http.getURIScheme() + "://" + url;
			url = "http://"+url;
		}

		url = url.replaceAll(" ", "%20"); // TODO escape more
		
		try {
			base = new URI(url);
		} catch (URISyntaxException e) {
			throw new ToolException("Malformed URL parameter: ", e);
		}
		base = base.normalize();
	}
}