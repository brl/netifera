package com.netifera.platform.net.tools.bruteforce;

import java.util.Collection;
import java.util.Iterator;

import com.netifera.platform.api.iterables.FiniteIterable;
import com.netifera.platform.api.iterables.IterableComposite;
import com.netifera.platform.net.services.credentials.Credential;
import com.netifera.platform.net.services.credentials.UsernameAndPassword;

public class UsernameAndPasswordGenerator implements FiniteIterable<Credential> {

	private IterableComposite<String> passwords = new IterableComposite<String>();
	private IterableComposite<String> usernames = new IterableComposite<String>();
	
	public void addPasswordList(FiniteIterable<String> list) {
		passwords.add(list);
	}

	public void addPasswordList(Collection<String> list) {
		passwords.add(list);
	}

	public void addUsernameList(FiniteIterable<String> list) {
		usernames.add(list);
	}

	public void addUsernameList(Collection<String> list) {
		usernames.add(list);
	}

	public int itemCount() {
		return passwords.itemCount() * usernames.itemCount();
	}

	public Iterator<Credential> iterator() {
		final Iterator<String> passwordsIterator = passwords.iterator();
		return new Iterator<Credential>() {
			private String password = passwordsIterator.next();
			private Iterator<String> usernamesIterator = usernames.iterator();
			
			public boolean hasNext() {
				return usernamesIterator.hasNext() || passwordsIterator.hasNext();
			}

			public UsernameAndPassword next() {
				if (!usernamesIterator.hasNext()) {
					password = passwordsIterator.next();
					if (password.equals("%null%"))
						password = "";
					usernamesIterator = usernames.iterator();
				}
				
				String username = usernamesIterator.next();
				if (username.equals("%null%"))
					username = "";

				return new UsernameAndPassword(username, password.contains("%username%") ? password.replaceAll("%username%", username) : password);
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

}
