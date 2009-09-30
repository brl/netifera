package com.netifera.platform.net.http.tools;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.xbill.DNS.TextParseException;

import com.netifera.platform.api.probe.IProbe;
import com.netifera.platform.api.tools.ITool;
import com.netifera.platform.api.tools.IToolContext;
import com.netifera.platform.api.tools.ToolException;
import com.netifera.platform.net.dns.service.nameresolver.INameResolver;
import com.netifera.platform.net.http.internal.tools.Activator;
import com.netifera.platform.net.http.service.HTTP;
import com.netifera.platform.net.http.spider.impl.WebSpider;
import com.netifera.platform.net.http.spider.modules.FaviconHarvesterModule;
import com.netifera.platform.net.http.spider.modules.HTTPBasicAuthExtractorModule;
import com.netifera.platform.tools.RequiredOptionMissingException;
import com.netifera.platform.util.addresses.inet.IPv4Address;
import com.netifera.platform.util.addresses.inet.IPv6Address;
import com.netifera.platform.util.addresses.inet.InternetAddress;
import com.netifera.platform.util.locators.TCPSocketLocator;
import com.netifera.platform.util.patternmatching.InternetAddressMatcher;

public class AddWebSite  implements ITool {
	
	private String url;
	private INameResolver resolver;
	
	private IToolContext context;
	private long realm;

	public void toolRun(IToolContext context) throws ToolException {
		this.context = context;

		// XXX hardcode local probe as realm
		IProbe probe = Activator.getInstance().getProbeManager().getLocalProbe();
		realm = probe.getEntity().getId();

		context.setTitle("Add web site");
		
		setupToolOptions();

		try {
			URI actualURL = new URI(url);
			context.setTitle("Add web site "+actualURL);
			int port = actualURL.getPort() == -1 ? 80 : actualURL.getPort();
			String host = actualURL.getHost();
			List<InternetAddress> addresses;
			if (InternetAddressMatcher.matches(host)) {
				addresses = new ArrayList<InternetAddress>(1);
				addresses.add(InternetAddress.fromString(host));
			} else {
				try {
					addresses = resolver.getAddressesByName(host);
				} catch (UnknownHostException e) {
					context.error("Unknown host: "+actualURL.getHost());
					return;
				} catch (TextParseException e) {
					context.error("Unable to parse host: "+actualURL.getHost());
					return;
				}
				for (InternetAddress address : addresses) {
					context.info(host+" has address "+address);
					if (address instanceof IPv6Address) {
						Activator.getInstance().getDomainEntityFactory().createAAAARecord(realm, context.getSpaceId(), host, (IPv6Address)address);
					} else {
						Activator.getInstance().getDomainEntityFactory().createARecord(realm, context.getSpaceId(), host, (IPv4Address)address);
					}
				}
			}
			
			for (InternetAddress address : addresses) {
/*				WebSiteEntity entity = Activator.getInstance().getWebEntityFactory().createWebSite(realm, context.getSpaceId(), new TCPSocketLocator(address, port), hostname);
				entity.addTag("Target");
				entity.update();
*/
				fetch(new HTTP(new TCPSocketLocator(address, port)), actualURL);
			}

			// TODO: add credentials if any
		} catch (URISyntaxException e) {
			context.error("Malformed URL: " + url);
		} finally {
			context.done();
		}
	}
	
	private void setupToolOptions() throws RequiredOptionMissingException {
		resolver = Activator.getInstance().getNameResolver();
		url = (String) context.getConfiguration().get("url");
		if (url == null) {
			throw new RequiredOptionMissingException("url");
		}
	}
	
	private void fetch(HTTP http, URI url) {
		try {
			WebSpider spider = new WebSpider();
			spider.setServices(context.getLogger(), Activator.getInstance().getWebEntityFactory(), Activator.getInstance().getNameResolver());
			spider.setRealm(realm);
			spider.setSpaceId(context.getSpaceId());
			spider.addTarget(http, url.getHost());
			spider.visit(url);

			spider.addModule(FaviconHarvesterModule.class.getName());
//			spider.addModule(EmailsHarvesterModule.class.getName());
			spider.addModule(HTTPBasicAuthExtractorModule.class.getName());
//			spider.addModule(CrawlBackupFilesModule.class.getName());
//			spider.addModule(CrawlDefaultFilesModule.class.getName());
			
			spider.setFollowLinks(false);
			spider.setFetchImages(false);
//			spider.setMaximumConnections((Integer)context.getConfiguration().get("maximumConnections"));
//			spider.setBufferSize((Integer)context.getConfiguration().get("bufferSize"));

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
}
