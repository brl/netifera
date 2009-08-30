package com.netifera.platform.net.http.web.model;

import com.netifera.platform.api.model.AbstractEntity;
import com.netifera.platform.api.model.IEntity;
import com.netifera.platform.api.model.IEntityReference;
import com.netifera.platform.api.model.IWorkspace;
import com.netifera.platform.net.model.ServiceEntity;
import com.netifera.platform.util.HexaEncoding;
import com.netifera.platform.util.addresses.inet.InternetAddress;

public class HTTPBasicAuthenticationEntity extends AbstractEntity {
	
	private static final long serialVersionUID = -8225385855212258380L;

	final public static String ENTITY_TYPE = "web.auth.basic";

	private final IEntityReference site;
	private final String authenticationRealm;
	
	public HTTPBasicAuthenticationEntity(IWorkspace workspace, long realm, IEntityReference site, String authenticationRealm) {
		super(ENTITY_TYPE, workspace, realm);
		
		this.site = site;
		this.authenticationRealm = authenticationRealm;
	}

	HTTPBasicAuthenticationEntity() {
		site = null;
		authenticationRealm = null;
	}
	
	public WebSiteEntity getWebSite() {
		return (WebSiteEntity) referenceToEntity(site);
	}
	
	public String getAuthenticationRealm() {
		return authenticationRealm;
	}
	
	@Override
	protected IEntity cloneEntity() {
		//FIXME
		return this;
	}
	
	public static String createQueryKey(long realmId, InternetAddress address, int port, String vhost, String authenticationRealm) {
		return ENTITY_TYPE + ":" + realmId + ":" + HexaEncoding.bytes2hex(address.toBytes()) + ":" + port + ":" + vhost + ":" + authenticationRealm;
	}
	
	@Override
	protected String generateQueryKey() {
		WebSiteEntity site = getWebSite();
		ServiceEntity http = site.getHTTP();
		return createQueryKey(getRealmId(), http.getAddress().getAddress(), http.getPort(), site.getHostName(), authenticationRealm);
	}

}
