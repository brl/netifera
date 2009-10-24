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
import com.netifera.platform.net.sockets.LineChannel;
import com.netifera.platform.net.sockets.TCPChannel;
import com.netifera.platform.util.asynchronous.CompletionHandler;
import com.netifera.platform.util.locators.TCPSocketLocator;

public class FTPAuthBruteforcer extends UsernameAndPasswordBruteforcer {
	private TCPSocketLocator target;
	
	protected void setupToolOptions() throws ToolException {
		target = (TCPSocketLocator) context.getConfiguration().get("target");
		context.setTitle("Bruteforce authentication on FTP @ "+target);
		super.setupToolOptions();
	}
	
	public void authenticationSucceeded(Credential credential) {
		UsernameAndPassword up = (UsernameAndPassword) credential;
		Activator.getInstance().getNetworkEntityFactory().createUsernameAndPassword(realm, context.getSpaceId(), target, up.getUsernameString(), up.getPasswordString());
		String username = up.getUsernameString();
		if (!username.equals("ftp") && !username.equals("anonymous")) {
			UserEntity user = Activator.getInstance().getNetworkEntityFactory().createUser(realm, context.getSpaceId(), target.getAddress(), username);
			user.setPassword(up.getPasswordString());
			user.update();
		}
		super.authenticationSucceeded(credential);
	}
	
	protected CredentialsVerifier createCredentialsVerifier() {
		TCPCredentialsVerifier verifier = new TCPCredentialsVerifier(target) {
			private void command(final LineChannel channel, final String cmd, final long timeout, final TimeUnit unit, final CompletionHandler<Integer,Void> handler) {
				channel.writeLine(cmd, timeout, unit, null, new CompletionHandler<Void,Void>() {
					public void completed(Void result, Void attachment) {
						channel.readLine(timeout, unit, attachment, new CompletionHandler<String,Void>() {
							public void completed(String result, Void attachment) {
								if (result.startsWith("220") || result.startsWith("500"))
									channel.readLine(timeout, unit, attachment, this);
								else if (result.matches("^\\d\\d\\d.*"))
									handler.completed(Integer.parseInt(result.substring(0,3)),attachment);
								else
									handler.failed(new IOException("Invalid FTP response: '"+result+"'"), attachment);
							}
							public void cancelled(Void attachment) {
								handler.cancelled(attachment);
							}
							public void failed(Throwable exc, Void attachment) {
								handler.failed(exc, attachment);
							}
						});
					}
					public void cancelled(Void attachment) {
						handler.cancelled(attachment);
					}
					public void failed(Throwable exc, Void attachment) {
						handler.failed(exc, attachment);
					}
				});
			}
			
			protected void authenticate(TCPChannel channel, final Credential credential,
					final long timeout, final TimeUnit unit,
					final CompletionHandler<Boolean, Credential> handler) {
				final LineChannel lineChannel = new LineChannel(channel);
				final UsernameAndPassword usernameAndPassword = (UsernameAndPassword) credential;
				command(lineChannel, "USER "+usernameAndPassword.getUsernameString(),timeout,unit,new CompletionHandler<Integer,Void>() {
					public void completed(Integer result, Void attachment) {
						if (result == 230) {
							// anonymous login
							handler.completed(true, new UsernameAndPassword(((UsernameAndPassword)credential).getUsernameString(),""));
							return;
						}
						command(lineChannel, "PASS "+usernameAndPassword.getPasswordString(),timeout,unit,new CompletionHandler<Integer,Void>() {
							public void completed(Integer result,
									Void attachment) {
								handler.completed(result == 230, credential);
							}
							public void cancelled(Void attachment) {
								handler.cancelled(credential);
							}
							public void failed(Throwable exc, Void attachment) {
								handler.failed(exc, credential);
							}
						});
					}
					public void cancelled(Void attachment) {
						handler.cancelled(credential);
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
