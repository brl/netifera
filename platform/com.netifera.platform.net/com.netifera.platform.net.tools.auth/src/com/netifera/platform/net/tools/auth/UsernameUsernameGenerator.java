package com.netifera.platform.net.tools.auth;

import java.util.Collection;
import java.util.Iterator;

import com.netifera.platform.api.iterables.FiniteIterable;
import com.netifera.platform.api.iterables.IterableComposite;
import com.netifera.platform.net.services.credentials.Credential;
import com.netifera.platform.net.services.credentials.UsernameAndPassword;

public class UsernameUsernameGenerator implements FiniteIterable<Credential> {

	private IterableComposite<String> usernames = new IterableComposite<String>();

	public void addUsernameList(FiniteIterable<String> list) {
		usernames.add(list);
	}

	public void addUsernameList(Collection<String> list) {
		usernames.add(list);
	}

	public int itemCount() {
		return usernames.itemCount();
	}

	public Iterator<Credential> iterator() {
		final Iterator<String> iterator = usernames.iterator();
		return new Iterator<Credential>() {
			public boolean hasNext() {
				return iterator.hasNext();
			}

			public UsernameAndPassword next() {
				String username = iterator.next();
				if (username == null) return null;
				return new UsernameAndPassword(username, username);
			}

			public void remove() {
				iterator.remove();
			}
		};
	}

}
