package com.netifera.platform.net.http.web.model;

import com.netifera.platform.api.model.AbstractEntity;
import com.netifera.platform.api.model.IEntity;
import com.netifera.platform.api.model.IEntityReference;
import com.netifera.platform.api.model.IWorkspace;
import com.netifera.platform.net.model.ServiceEntity;
import com.netifera.platform.util.HexaEncoding;
import com.netifera.platform.util.addresses.inet.InternetAddress;

public class WebApplicationEntity extends AbstractEntity {
	
	private static final long serialVersionUID = -2658295107593119953L;

	final public static String ENTITY_TYPE = "web.app";

	private final IEntityReference page;
	private final String serviceType;
	
	public WebApplicationEntity(IWorkspace workspace, long realm, IEntityReference page, String serviceType) {
		super(ENTITY_TYPE, workspace, realm);

		this.page = page;
		this.serviceType = serviceType;
	}
	
	WebApplicationEntity() {
		page = null;
		serviceType = null;
	}
	
	public WebPageEntity getWebPage() {
		return (WebPageEntity) referenceToEntity(page);
	}
	
	public String getServiceType() {
		return serviceType;
	}

	public void setVersion(String version) {
		setNamedAttribute("version", version);
	}
	
	public String getVersion() {
		return getNamedAttribute("version");
	}

/*	@Override
	protected void synchronizeEntity(AbstractEntity masterEntity) {
		WebApplicationEntity webApp = (WebApplicationEntity) masterEntity;
		path = webApp.getPath();
	}
*/
	@Override
	protected IEntity cloneEntity() {
		return new WebApplicationEntity(getWorkspace(),getRealmId(),page,serviceType);
	}
	
	public static String createQueryKey(long realmId, InternetAddress address, int port, String hostname, String path, String serviceType) {
		return ENTITY_TYPE + ":" + realmId + ":" + HexaEncoding.bytes2hex(address.toBytes()) + ":" + port + ":" + hostname + ":" + path + ":" + serviceType;
	}
	
	@Override
	protected String generateQueryKey() {
		WebPageEntity page = getWebPage();
		WebSiteEntity site = page.getWebSite();
		String path = page.getPath();
		String hostname = site.getHostName();
		ServiceEntity http = site.getHTTP();
		return createQueryKey(getRealmId(), http.getAddress().getAddress(), http.getPort(), hostname, path, serviceType);
	}
}
