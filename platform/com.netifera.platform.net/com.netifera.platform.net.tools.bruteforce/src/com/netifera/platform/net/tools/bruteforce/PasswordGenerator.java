package com.netifera.platform.net.tools.bruteforce;

import java.util.Collection;
import java.util.Iterator;

import com.netifera.platform.api.iterables.FiniteIterable;
import com.netifera.platform.api.iterables.IterableComposite;
import com.netifera.platform.net.services.credentials.Credential;
import com.netifera.platform.net.services.credentials.Password;

public class PasswordGenerator implements FiniteIterable<Credential> {

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
				String password = iterator.next();
				if (password == null) return null;
				if (password.equals("%null%"))
					password = "";
				if (password.contains("%username%"))
					password.replace("%username%", "");
				return new Password(password);
			}

			public void remove() {
				iterator.remove();
			}
		};
	}

}
