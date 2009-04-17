package com.netifera.platform.net.http.spider.modules;

import com.netifera.platform.net.dns.model.EmailAddressEntity;
import com.netifera.platform.net.http.internal.tools.Activator;
import com.netifera.platform.net.http.spider.HTTPRequest;
import com.netifera.platform.net.http.spider.HTTPResponse;
import com.netifera.platform.net.http.spider.IWebSpiderContext;
import com.netifera.platform.net.http.spider.IWebSpiderModule;
import com.netifera.platform.util.patternmatching.EmailCollector;

public class EmailsHarversterModule implements IWebSpiderModule {

	public String getName() {
		return "Harvest Email Addresses";
	}

	public void handle(IWebSpiderContext context, HTTPRequest request, HTTPResponse response) {
		if (response.getStatusCode() == 200) {
			if (response.getContentType().matches("(text/|application/x-javascript).*")) {
				String data = response.toString(); //NOTE wont necesarily get the hole content, there's a limit in HTTPResponseAdapter.DEFAULT_BUFFER_SIZE
				EmailCollector collector = new EmailCollector();
				collector.parse(data, EmailCollector.PARSE_ALL);
				for (String email: collector.results()) {
//					if (interrupted) return;
					EmailAddressEntity e = Activator.getInstance().getDomainEntityFactory().createEmailAddress(context.getRealm(), 0, email);
					e.addTag(context.getBaseURL().toString());
					e.save();
					e.addToSpace(context.getSpaceId());
				}
			}
		}
	}

	public void start(IWebSpiderContext context) {
	}

	public void stop() {
	}
}
