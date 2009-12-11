package com.netifera.platform.net.cifs.tools;

import com.netifera.platform.api.tools.ToolException;
import com.netifera.platform.net.cifs.internal.tools.Activator;
import com.netifera.platform.net.model.UserEntity;
import com.netifera.platform.net.services.auth.CredentialsVerifier;
import com.netifera.platform.net.services.auth.TCPCredentialsVerifier;
import com.netifera.platform.net.services.credentials.Credential;
import com.netifera.platform.net.services.credentials.UsernameAndPassword;
import com.netifera.platform.net.tools.bruteforce.UsernameAndPasswordBruteforcer;
import com.netifera.platform.util.locators.TCPSocketLocator;

public class LMAuthBruteforcer extends UsernameAndPasswordBruteforcer {
	private TCPSocketLocator target;
	private String remoteName = "*SMBSERVER";
	private String localName = "";
	
	@Override
	protected void setupToolOptions() throws ToolException {
		target = (TCPSocketLocator) context.getConfiguration().get("target");
		context.setTitle("Bruteforce LM authentication on SMB @ "+target);
		
		if (context.getConfiguration().get("remoteName") != null)
			remoteName = (String) context.getConfiguration().get("remoteName");
		if (context.getConfiguration().get("localName") != null)
			localName = (String) context.getConfiguration().get("localName");
		
		super.setupToolOptions();
	}
	
	@Override
	public void authenticationSucceeded(Credential credential) {
		UsernameAndPassword up = (UsernameAndPassword) credential;
		Activator.getInstance().getNetworkEntityFactory().createUsernameAndPassword(context.getRealm(), context.getSpaceId(), target, up.getUsernameString(), up.getPasswordString());
		
		UserEntity user = Activator.getInstance().getNetworkEntityFactory().createUser(context.getRealm(), context.getSpaceId(), target.getAddress(), up.getUsernameString());
		user.setPassword(up.getPasswordString());
//		user.setHash("LM", hash(credential));
		user.update();
		
		super.authenticationSucceeded(credential);
	}

	@Override
	public CredentialsVerifier createCredentialsVerifier() {
		TCPCredentialsVerifier verifier = new LMCredentialsVerifier(target, remoteName, localName);
		verifier.setMaximumConnections((Integer) context.getConfiguration().get("maximumConnections"));
		return verifier;
	}
}
