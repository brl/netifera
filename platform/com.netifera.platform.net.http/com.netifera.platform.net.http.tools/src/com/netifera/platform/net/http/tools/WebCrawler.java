package com.netifera.platform.net.http.tools;

import java.net.URI;
import java.net.URISyntaxException;

import com.netifera.platform.api.tools.ITool;
import com.netifera.platform.api.tools.IToolContext;
import com.netifera.platform.api.tools.ToolException;
import com.netifera.platform.net.http.internal.tools.Activator;
import com.netifera.platform.net.http.service.HTTP;
import com.netifera.platform.net.http.spider.impl.WebSpider;
import com.netifera.platform.tools.RequiredOptionMissingException;

public class WebCrawler implements ITool {
	private IToolContext context;
	private HTTP http;
	private URI base;

	public void run(IToolContext context) throws ToolException {
		this.context = context;

		context.setTitle("Web crawler");
		setupToolOptions();

		String host = base.getHost();
		if (host != null && host.compareTo(http.getURIHost()) == 0) {
			context.setTitle("Crawl "+base);
		} else {
			context.setTitle("Crawl "+base+" at "+http.getSocketAddress());
		}
		
		try {
			WebSpider spider = new WebSpider();
			spider.setServices(context.getLogger(), Activator.getInstance().getWebEntityFactory(), Activator.getInstance().getNameResolver());
			spider.setRealm(context.getRealm());
			spider.setSpaceId(context.getSpaceId());
			spider.addTarget(http, base.getHost());
			spider.visit(base);
			
			if (context.getConfiguration().get("createWebPageEntities") != null)
				spider.setCreateWebPageEntities((Boolean)context.getConfiguration().get("createWebPageEntities"));
			if (context.getConfiguration().get("buildLinksGraph") != null)
				spider.setBuildLinksGraph((Boolean)context.getConfiguration().get("buildLinksGraph"));
			if (context.getConfiguration().get("followLinks") != null)
				spider.setFollowLinks((Boolean)context.getConfiguration().get("followLinks"));
			if (context.getConfiguration().get("fetchImages") != null)
				spider.setFetchImages((Boolean)context.getConfiguration().get("fetchImages"));
			if (context.getConfiguration().get("maximumConnections") != null)
				spider.setMaximumConnections((Integer)context.getConfiguration().get("maximumConnections"));
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