package com.netifera.platform.net.tools.auth;

import java.util.Collection;
import java.util.Iterator;

import com.netifera.platform.api.iterables.FiniteIterable;
import com.netifera.platform.api.iterables.IterableComposite;
import com.netifera.platform.net.services.credentials.Credential;
import com.netifera.platform.net.services.credentials.Password;

public class PasswordIterable implements FiniteIterable<Credential> {

	private IterableComposite<String> passwords = new IterableComposite<String>();

	public void addPasswordList(FiniteIterable<String> list) {
		passwords.add(list);
	}

	public void addPasswordList(Collection<String> list) {
		passwords.add(list);
	}

	public int itemCount() {
		return passwords.itemCount();
	}

	public Iterator<Credential> iterator() {
		final Iterator<String> iterator = passwords.iterator();
		return new Iterator<Credential>() {
			public boolean hasNext() {
				return iterator.hasNext();
			}

			public Password next() {
				String passwordString = iterator.next();
				if (passwordString == null) return null;
				return new Password(passwordString);
			}

			public void remove() {
				iterator.remove();
			}
		};
	}

}
