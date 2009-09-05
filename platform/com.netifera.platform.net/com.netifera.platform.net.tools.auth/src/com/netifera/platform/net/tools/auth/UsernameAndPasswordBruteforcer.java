package com.netifera.platform.net.tools.auth;

import java.util.ArrayList;
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

		Boolean tryNullPassword = (Boolean) context.getConfiguration().get("tryNullPassword");
		if (tryNullPassword == null)
			tryNullPassword = false;
		
		Boolean tryUsernameAsPassword = (Boolean) context.getConfiguration().get("tryUsernameAsPassword");
		if (tryUsernameAsPassword == null)
			tryUsernameAsPassword = false;
		
		if (tryNullPassword || tryUsernameAsPassword) {
			UsernameAndPasswordGenerator userpass = new UsernameAndPasswordGenerator();
			userpass.addUsernameList(usernames);
			List<String> patterns = new ArrayList<String>();
			if (tryUsernameAsPassword)
				patterns.add("%username%");
			if (tryNullPassword)
				patterns.add("%null%");
			userpass.addPasswordList(patterns);
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
