package com.netifera.platform.net.tools.auth;

import java.util.Collection;
import java.util.Iterator;

import com.netifera.platform.api.iterables.FiniteIterable;
import com.netifera.platform.api.iterables.IterableComposite;
import com.netifera.platform.net.services.credentials.Credential;
import com.netifera.platform.net.services.credentials.UsernameAndPassword;

public class UsernameAndPasswordIterable implements FiniteIterable<Credential> {

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
		final Iterator<String> usernamesIterator = usernames.iterator();
		return new Iterator<Credential>() {
			private String currentUsername = usernamesIterator.next();
			private Iterator<String> passwordsIterator = passwords.iterator();
			
			public boolean hasNext() {
				return usernamesIterator.hasNext() || passwordsIterator.hasNext();
			}

			public UsernameAndPassword next() {
				if (!passwordsIterator.hasNext()) {
					currentUsername = usernamesIterator.next();
					passwordsIterator = passwords.iterator();
				}
				
				String password = passwordsIterator.next();
				if (password.equals("%null%"))
					password = "";
				if (password.contains("%username%"))
					password.replace("%username%", currentUsername);

				return new UsernameAndPassword(currentUsername, password);
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

}
