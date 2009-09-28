package com.netifera.platform.net.tools.bruteforce;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.netifera.platform.api.iterables.FiniteIterable;
import com.netifera.platform.api.iterables.IterableComposite;
import com.netifera.platform.net.internal.tools.bruteforce.Activator;
import com.netifera.platform.net.services.credentials.Credential;
import com.netifera.platform.net.wordlists.IWordList;

public abstract class UsernameAndPasswordBruteforcer extends AuthenticationBruteforcer {
	
	protected FiniteIterable<Credential> createCredentials() {
		IterableComposite<Credential> credentials = new IterableComposite<Credential>();

		IterableComposite<String> usernames = new IterableComposite<String>();
		IterableComposite<String> passwords = new IterableComposite<String>();

		if (((String)context.getConfiguration().get("usernames")).length() > 0)
			usernames.add(Arrays.asList(((String)context.getConfiguration().get("usernames")).split("[\\s,]+")));
		if (((String)context.getConfiguration().get("passwords")).length() > 0)
			passwords.add(Arrays.asList(((String)context.getConfiguration().get("passwords")).split("[\\s,]+")));

		for (String wordlistName: (String[]) context.getConfiguration().get("usernames_wordlists")) {
			IWordList wordlist = Activator.getInstance().getWordList(wordlistName);
			if (wordlist == null) {
				context.error("Missing wordlist: "+wordlistName);
			} else {
				FiniteIterable<String> words = wordlist.getWords();
				context.info("Added wordlist: "+wordlistName+" ["+words.itemCount()+" usernames]");
				usernames.add(words);
			}
		}

		for (String wordlistName: (String[]) context.getConfiguration().get("passwords_wordlists")) {
			IWordList wordlist = Activator.getInstance().getWordList(wordlistName);
			if (wordlist == null) {
				context.error("Missing wordlist: "+wordlistName);
			} else {
				FiniteIterable<String> words = wordlist.getWords();
				context.info("Added wordlist: "+wordlistName+" ["+words.itemCount()+" passwords]");
				passwords.add(words);
			}
		}

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

		if (passwords.itemCount() > 0) {
			UsernameAndPasswordGenerator userpass = new UsernameAndPasswordGenerator();
			userpass.addUsernameList(usernames);
			userpass.addPasswordList(passwords);
			credentials.add(userpass);
		}
		
		return credentials;
	}
}
