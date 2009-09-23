package com.netifera.platform.net.services.auth;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import com.netifera.platform.net.services.credentials.Credential;

public abstract class CredentialsVerifier {
	protected Iterator<Credential> credentials;
	private final Queue<Credential> retryCredentials = new LinkedList<Credential>();
	protected AuthenticationListener listener;
	private boolean canceled = false;

	protected boolean hasNextCredential() {
		return !canceled && (credentials.hasNext() || !retryCredentials.isEmpty());
	}
	
	protected Credential nextCredentialOrNull() {
		if (canceled) return null;
		synchronized(credentials) {
			synchronized(retryCredentials) {
				Credential retry = retryCredentials.poll();
				if (retry != null) return retry;
			}
			if (credentials.hasNext()) return credentials.next();
		}
		return null;
	}

	public void retryCredential(Credential credential) {
		synchronized(retryCredentials) {
			retryCredentials.add(credential);
		}
	}

	public abstract void tryCredentials(Iterator<Credential> credentials, AuthenticationListener listener) throws IOException, InterruptedException;
	
	public void cancel() {
		canceled = true;
	}
}
