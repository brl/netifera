package com.netifera.platform.net.ssh.tools;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.netifera.platform.api.tools.ToolException;
import com.netifera.platform.net.model.UserEntity;
import com.netifera.platform.net.services.auth.CredentialsVerifier;
import com.netifera.platform.net.services.credentials.Credential;
import com.netifera.platform.net.services.credentials.UsernameAndPassword;
import com.netifera.platform.net.services.ssh.SSH;
import com.netifera.platform.net.ssh.internal.tools.Activator;
import com.netifera.platform.net.tools.bruteforce.UsernameAndPasswordBruteforcer;
import com.netifera.platform.util.locators.TCPSocketLocator;
import com.trilead.ssh2.Connection;

public class SSHAuthBruteforcer extends UsernameAndPasswordBruteforcer {
	private TCPSocketLocator target;
	private int maximumConnections = 1;

	@Override
	protected void setupToolOptions() throws ToolException {
		target = (TCPSocketLocator) context.getConfiguration().get("target");
		maximumConnections = (Integer) context.getConfiguration().get("maximumConnections");

		context.setTitle("Bruteforce authentication on SSH @ " + target);
		
		super.setupToolOptions();
	}

	@Override
	public void authenticationSucceeded(Credential credential) {
		UsernameAndPassword up = (UsernameAndPassword) credential;
		Activator.getInstance().getNetworkEntityFactory().createUsernameAndPassword(context.getRealm(), context.getSpaceId(), target, up.getUsernameString(), up.getPasswordString());
		UserEntity user = Activator.getInstance().getNetworkEntityFactory().createUser(context.getRealm(), context.getSpaceId(), target.getAddress(), up.getUsernameString());
		user.setPassword(up.getPasswordString());
		user.update();
		super.authenticationSucceeded(credential);
	}

	@Override
	public CredentialsVerifier createCredentialsVerifier() {
		return new CredentialsVerifier() {
			volatile int errorCount = 0;
			volatile int successCount = 0;
			AtomicInteger connectionsCount = new AtomicInteger(0);

			@Override
			public void run() throws IOException, InterruptedException {
				ExecutorService executor = Executors.newFixedThreadPool(maximumConnections);

				try {
					final SSH ssh = new SSH(target);
					while (hasNextCredential()) {
						while (connectionsCount.get() >= maximumConnections)
							Thread.sleep(1000);
						Thread.sleep(500);
						connectionsCount.incrementAndGet();
						executor.submit(new Runnable() {
							public void run() {
								Connection connection = null;
								try {
									connection = ssh.createConnection();
									while (hasNextCredential()) {
										UsernameAndPassword credential = (UsernameAndPassword) nextCredentialOrNull();
										if (credential == null)
											return;

										try {
											boolean success = connection
													.authenticateWithPassword(
															credential.getUsernameString(),
															credential.getPasswordString());
											successCount = successCount + 1;
											if (success) {
												authenticationSucceeded(credential);
												connection.close();
												if (hasNextCredential()) {
													Thread.sleep(2000);
													connection = ssh.createConnection();
												}
											} else {
												authenticationFailed(credential);
											}
										} catch (IOException e) {
											context.error(e.getMessage() + (e.getCause() != null ? " ("+e.getCause().getMessage()+")" : ""));
//											e.printStackTrace();
											errorCount = errorCount + 1;
											authenticationError(credential, e);
											connection.close();
											if (errorCount / (successCount + 1) > 3) {
												context.error("Too many errors, aborting.");
												cancel();
												return;
											}
											if (hasNextCredential()) {
												Thread.sleep(1000);
												connection = ssh.createConnection();
											}
										}
									}
								} catch (IOException e) {
									context.error(e.getMessage() + (e.getCause() != null ? " ("+e.getCause().getMessage()+")" : ""));
//									e.printStackTrace();
									errorCount = errorCount + 1;
									if (errorCount / (successCount + 1) > 2) {
										context.error("Too many errors, aborting.");
										cancel();
									} else if (maximumConnections > 5) { // dont decrease it too much
										maximumConnections = maximumConnections - 1;
										context.error("Connection error, decreasing maximum parallel connections to " + maximumConnections);
									}
									return;
								} catch (InterruptedException e) {
//									e.printStackTrace();
									context.warning("Interrupted");
									cancel();
									Thread.interrupted();
								} finally {
									connectionsCount.decrementAndGet();
									if (connection != null)
										connection.close();
								}
							}
						});
					}
				} finally {
					cancel();
					context.debug("Shutting down thread executor pool");
					executor.shutdown();
					if (!executor.awaitTermination(60, TimeUnit.SECONDS))
						context.warning("Thread executor pool not terminated after 1 minute");
				}
			}
		};
	}
}
