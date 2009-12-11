package com.netifera.platform.net.services.auth;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;

import com.netifera.platform.net.services.credentials.Credential;
import com.netifera.platform.net.services.credentials.UsernameAndPassword;

public abstract class CredentialsVerifier {
	private Iterator<Credential> credentials;
	private final Queue<Credential> retryCredentials = new LinkedList<Credential>();
	private final Set<String> skipUsers = new HashSet<String>();
	private AuthenticationListener listener;
	private boolean canceled = false;

	public void setCredentials(Iterator<Credential> credentials) {
		this.credentials = credentials;
	}
	
	public void setListener(AuthenticationListener listener) {
		this.listener = listener;
	}
	
	protected boolean hasNextCredential() {
		return !canceled && (credentials.hasNext() || !retryCredentials.isEmpty());
	}
	
	protected Credential nextCredentialOrNull() {
		if (canceled) return null;
		synchronized(credentials) {
			synchronized(retryCredentials) {
				Credential retry = null;
				do {
					retry = retryCredentials.poll();
				} while (retry != null && isRepeated(retry));
				if (retry != null) return retry;
			}
			Credential credential = null;
			do {
				try {
					credential = credentials.next();
				} catch (NoSuchElementException e) {
					return null;
				}
			} while (credential != null && isRepeated(credential));
			return credential;
		}
	}

	private boolean isRepeated(Credential credential) {
		if (credential instanceof UsernameAndPassword) {
			return skipUsers.contains(((UsernameAndPassword) credential).getUsernameString());
		}
		return false;
	}

	public void markBadUser(String username) {
		synchronized (credentials) {
			skipUsers.add(username);
		}
	}
	
	public void retryCredential(Credential credential) {
		synchronized(retryCredentials) {
			retryCredentials.add(credential);
		}
	}
	
	protected void authenticationSucceeded(Credential credential) {
		listener.authenticationSucceeded(credential);
		if (credential instanceof UsernameAndPassword) {
			synchronized (credentials) {
				skipUsers.add(((UsernameAndPassword) credential).getUsernameString());
			}
		}
	}
	
	protected void authenticationFailed(Credential credential) {
		listener.authenticationFailed(credential);
	}
	
	protected void authenticationError(Credential credential, Throwable e) {
		listener.authenticationError(credential, e);
	}

	public abstract void run() throws IOException, InterruptedException;

	public void cancel() {
		canceled = true;
	}
}
