package com.netifera.platform.net.tools.bruteforce;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.netifera.platform.api.tools.ToolException;
import com.netifera.platform.net.internal.tools.bruteforce.Activator;
import com.netifera.platform.net.model.UserEntity;
import com.netifera.platform.net.services.auth.CredentialsVerifier;
import com.netifera.platform.net.services.auth.TCPCredentialsVerifier;
import com.netifera.platform.net.services.credentials.Credential;
import com.netifera.platform.net.services.credentials.UsernameAndPassword;
import com.netifera.platform.net.sockets.CompletionHandler;
import com.netifera.platform.net.sockets.LineChannel;
import com.netifera.platform.net.sockets.TCPChannel;
import com.netifera.platform.util.locators.TCPSocketLocator;

public class IMAPAuthBruteforcer extends UsernameAndPasswordBruteforcer {
	private TCPSocketLocator target;
	
	@Override
	protected void setupToolOptions() throws ToolException {
		target = (TCPSocketLocator) context.getConfiguration().get("target");
		context.setTitle("Bruteforce authentication on IMAP @ "+target);
		super.setupToolOptions();
	}
	
	@Override
	public void authenticationSucceeded(Credential credential) {
		UsernameAndPassword up = (UsernameAndPassword) credential;
		Activator.getInstance().getNetworkEntityFactory().createUsernameAndPassword(realm, context.getSpaceId(), target, up.getUsernameString(), up.getPasswordString());
		
		//XXX is this good? the imap user might not be a local user
		UserEntity user = Activator.getInstance().getNetworkEntityFactory().createUser(realm, context.getSpaceId(), target.getAddress(), up.getUsernameString());
		user.setPassword(up.getPasswordString());
		user.update();
		
		super.authenticationSucceeded(credential);
	}
	
	@Override
	public CredentialsVerifier createCredentialsVerifier() {
		TCPCredentialsVerifier verifier = new TCPCredentialsVerifier(target) {
			@Override
			protected void authenticate(final TCPChannel channel, final Credential credential,
					final long timeout, final TimeUnit unit,
					final CompletionHandler<Boolean, Credential> handler) {
				final LineChannel lineChannel = new LineChannel(channel);
				lineChannel.setSeparator("\n");
				lineChannel.writeLine("1 LOGIN "+((UsernameAndPassword)credential).getUsernameString()+" "+((UsernameAndPassword)credential).getPasswordString(), timeout, unit, null, new CompletionHandler<Void,Void>() {
					public void cancelled(Void attachment) {
						handler.cancelled(credential);
					}
					public void completed(Void result, Void attachment) {
						lineChannel.readLine(timeout, unit, attachment, new CompletionHandler<String,Void>() {
							public void cancelled(Void attachment) {
								handler.cancelled(credential);
							}
							public void completed(String result, Void attachment) {
								if (!result.startsWith("1 ")) {
//									context.warning("bad result: "+result);
									lineChannel.readLine(timeout, unit, attachment, this);
								} else {
									boolean authenticated = result.startsWith("1 OK");
									handler.completed(authenticated,credential);
									if (authenticated)
										try {
											channel.close();
										} catch (IOException e) {
											context.exception("I/O Error", e);
										}
								}
							}
							public void failed(Throwable exc, Void attachment) {
								handler.failed(exc, credential);
							}
						});
					}
					public void failed(Throwable exc, Void attachment) {
						handler.failed(exc, credential);
					}
				});
			}
		};
		
		verifier.setMaximumConnections((Integer) context.getConfiguration().get("maximumConnections"));
		return verifier;
	}
}
