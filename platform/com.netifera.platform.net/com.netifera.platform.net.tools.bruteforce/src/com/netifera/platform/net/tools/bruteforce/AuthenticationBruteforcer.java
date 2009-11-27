package com.netifera.platform.net.tools.bruteforce;

import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import com.netifera.platform.api.iterables.FiniteIterable;
import com.netifera.platform.api.tools.ITool;
import com.netifera.platform.api.tools.IToolContext;
import com.netifera.platform.api.tools.ToolException;
import com.netifera.platform.net.services.auth.AuthenticationListener;
import com.netifera.platform.net.services.auth.CredentialsVerifier;
import com.netifera.platform.net.services.credentials.Credential;

public abstract class AuthenticationBruteforcer implements ITool, AuthenticationListener {
	private FiniteIterable<Credential> credentials;
	private Iterator<Credential> credentialsIterator;
	private CredentialsVerifier verifier;
	
	private boolean singleMode = false;

	protected IToolContext context;

	protected abstract CredentialsVerifier createCredentialsVerifier();
	protected abstract FiniteIterable<Credential> createCredentials();
	
	public void toolRun(IToolContext context) throws ToolException {
		this.context = context;

		setupToolOptions();
		context.setTotalWork(credentials.size());

		context.info("Trying "+credentials.size()+" credentials...");
		
		try {
			verifier = createCredentialsVerifier();
			verifier.setCredentials(credentialsIterator);
			verifier.setListener(this);
			verifier.run();
		} catch (IOException e) {
			context.exception("I/O Error", e);
		} catch (InterruptedException e) {
			context.warning("Interrupted");
			Thread.currentThread().interrupt();
		} finally {
//			verifier.close();
			context.done();
		}
	}
	
//	@SuppressWarnings("unchecked")
	protected void setupToolOptions() throws ToolException {
//		credentials = (IndexedIterable<Credential>) context.getConfiguration().get("credentials");
//		if (credentials == null)
			credentials = createCredentials();
		try {
			credentialsIterator = credentials.iterator();
		} catch (NoSuchElementException e) {
			throw new ToolException("No credentials to try");
		}

		Boolean singleMode = (Boolean) context.getConfiguration().get("singleMode");
		if (singleMode != null)
			this.singleMode = singleMode;
	}

	public void authenticationError(Credential credential, Throwable e) {
		String msg = e.getLocalizedMessage();
		if (msg == null) msg = e.getMessage();
		if (msg == null) msg = e.toString();
		context.debug("Retrying '"+credential+"' after error: "+msg);
		verifier.retryCredential(credential);
	}

	public void authenticationFailed(Credential credential) {
		context.debug("Invalid credential: "+credential);
		context.worked(1);
	}

	public void authenticationSucceeded(Credential credential) {
		context.info("Found valid credential: "+credential);
		context.worked(1);
		
		if (singleMode)
			verifier.cancel();
	}
}
