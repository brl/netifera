package com.netifera.platform.net.ssh.deploy;

import java.io.IOException;
import java.io.InputStream;

import com.netifera.platform.api.probe.IProbe;
import com.netifera.platform.api.tools.ITool;
import com.netifera.platform.api.tools.IToolContext;
import com.netifera.platform.api.tools.ToolException;
import com.netifera.platform.net.services.credentials.Credential;
import com.netifera.platform.net.services.credentials.UsernameAndPassword;
import com.netifera.platform.net.ssh.internal.deploy.Activator;
import com.netifera.platform.util.locators.TCPSocketLocator;
import com.netifera.probebuild.api.IProbeConfiguration;
import com.netifera.probebuild.api.IProbeDeployable;

public class SSHProbeDeployer implements ITool {

	private IToolContext context;
	private long realm;

	private TCPSocketLocator target;
	private Credential credential;

	private String probeConfigName;
	
	
	public void toolRun(IToolContext context) throws ToolException {
		this.context = context;

		// XXX hardcode local probe as realm
		IProbe probe = Activator.getInstance().getProbeManager().getLocalProbe();
		realm = probe.getEntity().getId();

		setupToolOptions();

		IProbeConfiguration probeConfig;
		try {
			probeConfig = Activator.getInstance().getProbeBuilder().getProbeConfiguration(probeConfigName);
			System.out.println("got probe config: "+probeConfig);
		} catch (IOException e) {
			throw new ToolException("Error while retrieving probe configuration '"+probeConfigName+"'", e);
		}

//		SSH ssh = new SSH(target);
//		Connection connection = ssh.createConnection(credential);

//		context.setTotalWork(...);
		context.info("Deploying probe...");

		IProbeDeployable deployable = Activator.getInstance().getProbeBuilder().getProbeDeployable("ELF32 Executable linux/i386");
		try {
			InputStream stream = deployable.getInputStream(probeConfig);
			byte[] buffer = new byte[1024*1024];
			int count = 0;
			int result;
			while ((result = stream.read(buffer)) > 0)
				count += result;
			System.out.println("size = "+count);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
/*		try {
//			verifier = createCredentialsVerifier();
//			verifier.tryCredentials(credentialsIterator, this);
		} catch (IOException e) {
			context.exception("I/O Error", e);
		} catch (InterruptedException e) {
			context.warning("Interrupted");
			Thread.currentThread().interrupt();
		} finally {
			context.done();
		}
*/	}
	
	protected void setupToolOptions() {
		target = (TCPSocketLocator) context.getConfiguration().get("target");
		
		credential = (Credential) context.getConfiguration().get("credential");
		if (credential == null) {
			String username = (String) context.getConfiguration().get("username");
			String password = (String) context.getConfiguration().get("password");
			credential = new UsernameAndPassword(username, password);
		}

		probeConfigName = (String) context.getConfiguration().get("probeConfig");
	}
}
