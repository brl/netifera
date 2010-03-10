package com.netifera.platform.net.http.internal.web.model;

import java.net.URI;
import java.util.Map;

import com.netifera.platform.api.model.IModelService;
import com.netifera.platform.api.model.IWorkspace;
import com.netifera.platform.net.http.web.model.BasicAuthenticationEntity;
import com.netifera.platform.net.http.web.model.HTTPRequestEntity;
import com.netifera.platform.net.http.web.model.HTTPResponseEntity;
import com.netifera.platform.net.http.web.model.IWebEntityFactory;
import com.netifera.platform.net.http.web.model.WebApplicationEntity;
import com.netifera.platform.net.http.web.model.WebFormAuthenticationEntity;
import com.netifera.platform.net.http.web.model.WebPageEntity;
import com.netifera.platform.net.http.web.model.WebSiteEntity;
import com.netifera.platform.net.model.ClientEntity;
import com.netifera.platform.net.model.ClientServiceConnectionEntity;
import com.netifera.platform.net.model.INetworkEntityFactory;
import com.netifera.platform.net.model.ServiceEntity;
import com.netifera.platform.util.addresses.inet.InternetAddress;
import com.netifera.platform.util.addresses.inet.TCPSocketAddress;

public class WebEntityFactory implements IWebEntityFactory {

	private IModelService model;
	private INetworkEntityFactory networkEntityFactory;
//	private IDomainEntityFactoryService domainEntityFactory;

	private IWorkspace getWorkspace() {
		if(model.getCurrentWorkspace() == null) {
			throw new IllegalStateException("Cannot create web entities because no workspace is currently open");
		}
		return model.getCurrentWorkspace();
	}

	protected void setModelService(IModelService model) {
		this.model = model;
	}

	protected void unsetModelService(IModelService model) {
		this.model = null;
	}
	
	protected void setNetworkEntityFactory(INetworkEntityFactory factory) {
		networkEntityFactory = factory;
	}

	protected void unsetNetworkEntityFactory(INetworkEntityFactory factory) {
		networkEntityFactory = null;
	}

	public synchronized ServiceEntity createWebServer(final long realm, long spaceId, TCPSocketAddress socketAddress, String product) {
		ServiceEntity entity = ServiceEntity.create(getWorkspace(), realm, spaceId, socketAddress, "HTTP");
		if (product != null && entity.setAttribute(ServiceEntity.PRODUCT_KEY, product))
			entity.update();
		return entity;
	}
	
	// TODO add Domain from hostname
	public synchronized WebSiteEntity createWebSite(final long realm, long spaceId,
			TCPSocketAddress socketAddress, String hostname) {
		return WebSiteEntity.create(getWorkspace(), realm, spaceId, socketAddress, hostname);
	}

	//XXX space not used? should notify?
	public synchronized void setFavicon(long realm, long space, TCPSocketAddress http,
			URI url, byte[] faviconBytes) {
		url = url.normalize();
		WebSiteEntity webSite = createWebSite(realm, space, http, url.getHost());
		webSite.setFavicon(faviconBytes);
		webSite.update();
	}

	public synchronized WebPageEntity createWebPage(final long realm, long spaceId, TCPSocketAddress http,
			URI url, String contentType) {
		url = url.normalize();
		final WebSiteEntity site = createWebSite(realm, spaceId, http, url.getHost());
		String path = url.getPath();
		if (path.length() == 0)
			path = "/";
		WebPageEntity answer = (WebPageEntity) getWorkspace().findByKey(WebPageEntity.createQueryKey(realm, http.getNetworkAddress(), http.getPort(), site.getHostName(), path));
		if (answer != null) {
			if (contentType != null && !contentType.equals(answer.getContentType())) {
				answer.setContentType(contentType);
				answer.update();
			}
			answer.addToSpace(spaceId);
			return answer;
		}
		
		answer = new WebPageEntity(getWorkspace(), realm, site, path, contentType);
		answer.save();
		answer.addToSpace(spaceId);
		return answer;
	}

	public synchronized WebApplicationEntity createWebApplication(final long realm, long spaceId, TCPSocketAddress http,
			URI url, Map<String, String> info) {
		String serviceType = info.get("serviceType");
		WebPageEntity page = createWebPage(realm, spaceId, http, url, null);
		WebApplicationEntity answer = (WebApplicationEntity) getWorkspace().findByKey(WebApplicationEntity.createQueryKey(realm, http.getNetworkAddress(), http.getPort(), page.getWebSite().getHostName(), page.getPath(), serviceType));
		if (answer != null) {
			if (info.get("version") != null) {
				answer.setVersion(info.get("version"));
				answer.save();
				answer.update();
			}
			answer.addToSpace(spaceId);
			return answer;
		}
		
		answer = new WebApplicationEntity(getWorkspace(), realm, page.createReference(), serviceType);
		if (info.get("version") != null)
			answer.setVersion(info.get("version"));
		answer.save();
		answer.update();
		answer.addToSpace(spaceId);
		return answer;
		
/*		url = url.normalize();
		final ServiceEntity service = createWebServer(realm, spaceId, http, null);
		final String urlString = url.toString();
		List<WebApplicationEntity> results = getWorkspace().findByPredicate(WebApplicationEntity.class,
			new IModelPredicate<WebApplicationEntity>() {
				public boolean match(WebApplicationEntity candidate) {
					return candidate.getRealmId() == realm && candidate.getHTTP() == service && (candidate.getURL().startsWith(urlString) || urlString.startsWith(candidate.getURL()));
				}
			});
		if(results.size() > 0) {
			WebApplicationEntity answer = results.get(0);
			if (!answer.getURL().equals(urlString) && answer.getURL().startsWith(urlString)) {
				answer.setURL(urlString);
				answer.update();
			}
			answer.addToSpace(spaceId);
			return answer;
		}
				
		WebApplicationEntity answer = new WebApplicationEntity(getWorkspace(), realm, service.createReference(), urlString, info.get("serviceType"));
		if (info.get("version") != null)
			answer.setVersion(info.get("version"));
		answer.save();
		answer.addToSpace(spaceId);
		return answer;
*/	}

	public synchronized WebPageEntity createWebPageWithBasicAuthentication(final long realm, long spaceId,
			TCPSocketAddress http, URI url, final String authenticationRealm) {

		url = url.normalize();
//		final ServiceEntity service = createWebServer(realm, spaceId, http, null);

		final WebSiteEntity site = createWebSite(realm, spaceId, http, url.getHost());

		BasicAuthenticationEntity answer = (BasicAuthenticationEntity) getWorkspace().findByKey(BasicAuthenticationEntity.createQueryKey(realm, http.getNetworkAddress(), http.getPort(), site.getHostName(), authenticationRealm));
		if (answer != null) {
			answer.addToSpace(spaceId);
		} else {
			answer = new BasicAuthenticationEntity(getWorkspace(), realm, site.createReference(), authenticationRealm);
			answer.save();
			answer.addToSpace(spaceId);
		}
		WebPageEntity page = createWebPage(realm, spaceId, http, url, null);
		page.setAuthentication(answer);
		page.update();
		return page;
	}

	public synchronized BasicAuthenticationEntity createBasicAuthentication(final long realm, long spaceId,
			TCPSocketAddress http, String hostname, final String authenticationRealm) {

		final WebSiteEntity site = createWebSite(realm, spaceId, http, hostname);

		BasicAuthenticationEntity answer = (BasicAuthenticationEntity) getWorkspace().findByKey(BasicAuthenticationEntity.createQueryKey(realm, http.getNetworkAddress(), http.getPort(), site.getHostName(), authenticationRealm));
		if (answer != null) {
			answer.addToSpace(spaceId);
		} else {
			answer = new BasicAuthenticationEntity(getWorkspace(), realm, site.createReference(), authenticationRealm);
			answer.save();
			answer.addToSpace(spaceId);
		}
		return answer;
	}

	@Deprecated // TODO
	public synchronized WebFormAuthenticationEntity createFormAuthentication(long realm, long space,
			TCPSocketAddress http, URI url, String usernameField,
			String passwordField) {
		// TODO Auto-generated method stub
		return null;
	}
	
	// TODO split createRequest+createResponse for (sniffed) asymetric traffic
	public synchronized HTTPRequestEntity createRequestResponse(final long realm, long spaceId,
			InternetAddress clientAddress, Map<String,String> clientInfo, TCPSocketAddress http,
			String requestLine, String responseStatusLine, String contentType) {
		
		ServiceEntity service = createWebServer(realm, spaceId, http, null);
		ClientEntity client = networkEntityFactory.createClient(realm, spaceId, clientAddress, "HTTP", clientInfo, null);
		ClientServiceConnectionEntity connection = networkEntityFactory.createConnection(spaceId, client, service, null);
		
		HTTPRequestEntity answer = new HTTPRequestEntity(getWorkspace(), connection, requestLine);
		HTTPResponseEntity response = new HTTPResponseEntity(getWorkspace(), connection, responseStatusLine);
		if (contentType != null)
			response.setAttribute("Content-Type", contentType);
		response.save();
		response.addToSpace(spaceId);
		answer.setResponse(response);
		answer.save();
		answer.addToSpace(spaceId);
		return answer;
	}
}
