package com.netifera.platform.net.http.tools;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.netifera.platform.net.http.internal.tools.Activator;
import com.netifera.platform.net.http.service.HTTP;
import com.netifera.platform.net.http.web.model.WebSiteEntity;
import com.netifera.platform.net.services.auth.CredentialsVerifier;
import com.netifera.platform.net.services.auth.TCPCredentialsVerifier;
import com.netifera.platform.net.services.credentials.Credential;
import com.netifera.platform.net.services.credentials.UsernameAndPassword;
import com.netifera.platform.net.sockets.CompletionHandler;
import com.netifera.platform.net.sockets.LineChannel;
import com.netifera.platform.net.sockets.TCPChannel;
import com.netifera.platform.net.tools.bruteforce.UsernameAndPasswordBruteforcer;
import com.netifera.platform.util.Base64;

public class HTTPBasicAuthBruteforcer extends UsernameAndPasswordBruteforcer {
	private HTTP target;
	private String hostname;
	private String path;
	private String method;
	
	@Override
	protected void setupToolOptions() {
		super.setupToolOptions();
		target = (HTTP) context.getConfiguration().get("target");
		path = (String) context.getConfiguration().get("path");
		hostname = (String) context.getConfiguration().get("hostname");
		method = (String) context.getConfiguration().get("method");
		context.setTitle("Bruteforce Basic HTTP authentication on "+target.getLocator()+" with "+path);
	}

	@Override
	public void authenticationSucceeded(Credential credential) {
		UsernameAndPassword up = (UsernameAndPassword) credential;
		WebSiteEntity webSiteEntity = Activator.getInstance().getWebEntityFactory().createWebSite(realm, context.getSpaceId(), target.getLocator(), hostname);
		Activator.getInstance().getNetworkEntityFactory().createUsernameAndPassword(realm, context.getSpaceId(), webSiteEntity, up.getUsernameString(), up.getPasswordString());
		super.authenticationSucceeded(credential);
	}
	
	@Override
	public CredentialsVerifier createCredentialsVerifier() {
		TCPCredentialsVerifier verifier = new TCPCredentialsVerifier(target.getLocator()) {
			@Override
			protected void authenticate(final TCPChannel channel, final Credential credential,
					final long timeout, final TimeUnit unit,
					final CompletionHandler<Boolean, Credential> handler) {
				final LineChannel lineChannel = new LineChannel(channel);
				final UsernameAndPassword usernameAndPassword = (UsernameAndPassword) credential;
				String request = method+" "+path+" HTTP/1.0\r\n";
				if (hostname != null && hostname.length()>0)
					request += "Host: "+hostname+"\r\n";
				request += "Authorization: Basic "+encode(usernameAndPassword)+"\r\n";
				lineChannel.writeLine(request, 5, TimeUnit.SECONDS, null, new CompletionHandler<Void,Void>() {
					public void completed(Void result, Void attachment) {
						lineChannel.readLine(5, TimeUnit.SECONDS, attachment, new CompletionHandler<String,Void>() {
							public void completed(String result, Void attachment) {
								context.debug("got: "+result);
								handler.completed(result.matches("HTTP/1\\.[01] 200.*"),credential);
								closeChannel();
							}
							public void cancelled(Void attachment) {
								handler.cancelled(credential);
								closeChannel();
							}
							public void failed(Throwable exc, Void attachment) {
								handler.failed(exc, credential);
								closeChannel();
							}
						});
					}
					public void cancelled(Void attachment) {
						handler.cancelled(credential);
						closeChannel();
					}
					public void failed(Throwable exc, Void attachment) {
						handler.failed(exc, credential);
						closeChannel();
					}
					private void closeChannel() {
						try {
							channel.close();
						} catch (IOException e) {
						}
					}
				});
			}
			
			String encode(UsernameAndPassword credential) {
				String userAndPassString = credential.getUsernameString()+":"+credential.getPasswordString();
				return Base64.encodeBytes(userAndPassString.getBytes());
			}
		};
		
		verifier.setMaximumConnections((Integer) context.getConfiguration().get("maximumConnections"));
		return verifier;
	}
}
