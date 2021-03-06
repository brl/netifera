package com.netifera.platform.net.cifs.tools;

import com.netifera.platform.api.tools.ToolException;
import com.netifera.platform.net.cifs.internal.tools.Activator;
import com.netifera.platform.net.model.UserEntity;
import com.netifera.platform.net.services.auth.CredentialsVerifier;
import com.netifera.platform.net.services.auth.TCPCredentialsVerifier;
import com.netifera.platform.net.services.credentials.Credential;
import com.netifera.platform.net.services.credentials.UsernameAndPassword;
import com.netifera.platform.net.tools.bruteforce.UsernameAndPasswordBruteforcer;
import com.netifera.platform.util.addresses.inet.TCPSocketAddress;

public class NTLMAuthBruteforcer extends UsernameAndPasswordBruteforcer {
	private TCPSocketAddress target;
	private String remoteName = "*SMBSERVER";
	private String localName = "";
	
	private boolean checkLocal = true;
	private boolean checkDomain = true;
	
	@Override
	protected void setupToolOptions() throws ToolException {
		target = (TCPSocketAddress) context.getConfiguration().get("target");
		context.setTitle("Bruteforce NTLM authentication on SMB @ "+target);
		
		if (context.getConfiguration().get("remoteName") != null)
			remoteName = (String) context.getConfiguration().get("remoteName");
		if (context.getConfiguration().get("localName") != null)
			localName = (String) context.getConfiguration().get("localName");

		if (context.getConfiguration().get("checkLocal") != null)
			checkLocal = (Boolean) context.getConfiguration().get("checkLocal");
		if (context.getConfiguration().get("checkDomain") != null)
			checkDomain = (Boolean) context.getConfiguration().get("checkDomain");
		
		super.setupToolOptions();
	}
	
	@Override
	public void authenticationSucceeded(Credential credential) {
		UsernameAndPassword up = (UsernameAndPassword) credential;
		Activator.getInstance().getNetworkEntityFactory().createUsernameAndPassword(context.getRealm(), context.getSpaceId(), target, up.getUsernameString(), up.getPasswordString());
		
		UserEntity user = Activator.getInstance().getNetworkEntityFactory().createUser(context.getRealm(), context.getSpaceId(), target.getNetworkAddress(), up.getUsernameString());
		user.setPassword(up.getPasswordString());
		user.update();
		
		super.authenticationSucceeded(credential);
	}

	@Override
	public CredentialsVerifier createCredentialsVerifier() {
		TCPCredentialsVerifier verifier = new NTLMCredentialsVerifier(target, remoteName, localName, checkLocal, checkDomain, context.getLogger());
		verifier.setMaximumConnections((Integer) context.getConfiguration().get("maximumConnections"));
		return verifier;
	}
	
}
