package com.netifera.platform.net.tools.bruteforce;

import com.netifera.platform.api.tools.ToolException;
import com.netifera.platform.net.internal.tools.bruteforce.Activator;
import com.netifera.platform.net.model.UserEntity;
import com.netifera.platform.net.services.auth.CredentialsVerifier;
import com.netifera.platform.net.services.credentials.Credential;
import com.netifera.platform.net.services.credentials.UsernameAndPassword;
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
		Activator.getInstance().getNetworkEntityFactory().createUsernameAndPassword(context.getRealm(), context.getSpaceId(), target, up.getUsernameString(), up.getPasswordString());
		String username = up.getUsernameString();
		if (!username.equals("ftp") && !username.equals("anonymous")) {
			UserEntity user = Activator.getInstance().getNetworkEntityFactory().createUser(context.getRealm(), context.getSpaceId(), target.getAddress(), username);
			user.setPassword(up.getPasswordString());
			user.update();
		}
		super.authenticationSucceeded(credential);
	}
	
	protected CredentialsVerifier createCredentialsVerifier() {
		FTPCredentialsVerifier verifier = new FTPCredentialsVerifier(target);
		verifier.setMaximumConnections((Integer) context.getConfiguration().get("maximumConnections"));
		return verifier;
	}
}
