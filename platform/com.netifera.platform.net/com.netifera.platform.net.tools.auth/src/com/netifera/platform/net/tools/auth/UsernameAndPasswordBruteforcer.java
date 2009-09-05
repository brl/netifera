package com.netifera.platform.net.tools.auth;

import java.util.Arrays;
import java.util.List;

import com.netifera.platform.api.iterables.FiniteIterable;
import com.netifera.platform.api.iterables.IterableComposite;
import com.netifera.platform.net.services.credentials.Credential;

public abstract class UsernameAndPasswordBruteforcer extends AuthenticationBruteforcer {
	
	protected FiniteIterable<Credential> createCredentials() {
		IterableComposite<Credential> credentials = new IterableComposite<Credential>();

		List<String> usernames = Arrays.asList(((String)context.getConfiguration().get("usernames")).split("[\\s,]+"));
		List<String> passwords = Arrays.asList(((String)context.getConfiguration().get("passwords")).split("[\\s,]+"));

		if ((Boolean) context.getConfiguration().get("tryUsernameAsPassword")) {
			UsernameUsernameGenerator useruser = new UsernameUsernameGenerator();
			useruser.addUsernameList(usernames);
			credentials.add(useruser);
		}
		
		if ((Boolean) context.getConfiguration().get("tryNullPassword")) {
			UsernameAndPasswordGenerator userpass = new UsernameAndPasswordGenerator();
			userpass.addUsernameList(usernames);
			userpass.addPasswordList(Arrays.asList(new String[] {""}));
			credentials.add(userpass);
		}

		if (passwords.size() > 0) {
			UsernameAndPasswordGenerator userpass = new UsernameAndPasswordGenerator();
			userpass.addUsernameList(usernames);
			userpass.addPasswordList(passwords);
			credentials.add(userpass);
		}
		
		return credentials;
	}
}
