package com.netifera.platform.net.ssh.tools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.netifera.platform.api.probe.IProbe;
import com.netifera.platform.api.tools.ITool;
import com.netifera.platform.api.tools.IToolContext;
import com.netifera.platform.api.tools.ToolException;
import com.netifera.platform.net.model.InternetAddressEntity;
import com.netifera.platform.net.services.credentials.Credential;
import com.netifera.platform.net.services.credentials.UsernameAndPassword;
import com.netifera.platform.net.services.ssh.SSH;
import com.netifera.platform.net.ssh.internal.tools.Activator;
import com.netifera.platform.util.locators.TCPSocketLocator;
import com.netifera.platform.probebuild.api.IProbeConfiguration;
import com.netifera.platform.probebuild.api.IProbeDeployable;
import com.trilead.ssh2.Connection;
import com.trilead.ssh2.SCPClient;
import com.trilead.ssh2.Session;

public class SSHProbeDeployer implements ITool {

	private IToolContext context;
	private long realm;

	private TCPSocketLocator target;
	private Credential credential;

	private String probeConfigName;
	private String probeName;
	
	public void toolRun(IToolContext context) throws ToolException {
		this.context = context;

		// XXX hardcode local probe as realm
		IProbe probe = Activator.getInstance().getProbeManager().getLocalProbe();
		realm = probe.getEntity().getId();

		context.setTitle("Deploy Probe via SSH");
		
		setupToolOptions();

		context.setTitle("Deploy Probe via SSH to "+target);

		IProbeConfiguration probeConfig;
		try {
			probeConfig = Activator.getInstance().getProbeBuilder().getProbeConfiguration(probeConfigName);
		} catch (IOException e) {
			throw new ToolException("Error while retrieving probe configuration '"+probeConfigName+"'", e);
		}

		SSH ssh = new SSH(target);
		Connection connection;
		try {
			connection = ssh.createConnection(credential);
		} catch (IOException e) {
			throw new ToolException("Connection failed",e);
		}

		context.info("Generating probe");
		
		IProbeDeployable deployable = Activator.getInstance().getProbeBuilder().getProbeDeployable("ELF32 Executable linux/i386");
		
		byte[] buffer = new byte[1024*1024];
		try {
			InputStream inputStream = deployable.getInputStream(probeConfig);
			File file = File.createTempFile("probe", "ssh-deploy");
			try {
//				context.setTotalWork(...);
				FileOutputStream outputStream = new FileOutputStream(file);
				int count = 0;
				int result;
				while ((result = inputStream.read(buffer)) > 0) {
					count += result;
					outputStream.write(buffer, 0, result);
				}
				outputStream.close();
				context.info("Probe successfuly generated, "+count+" bytes");

				context.info("Uploading probe via SCP");
				SCPClient scp = connection.createSCPClient();
				scp.put(file.getAbsolutePath(), "probe", "/tmp", "0777");
				context.info("Probe successfuly uploaded");
				
				context.info("Executing probe");
				Session session = connection.openSession();
				session.execCommand("/tmp/probe");
				
//				context.info("Connecting to the remote probe");
				
				InternetAddressEntity addressEntity = Activator.getInstance().getNetworkEntityFactory().createAddress(realm, context.getSpaceId(), target.getAddress());
				String channelConfig = "tcplisten:"+target.getAddress()+":31337";
				Activator.getInstance().getProbeManager().createProbe(addressEntity.getHost(), probeName != null && probeName.length()>0 ? probeName : "Remote Probe", channelConfig, context.getSpaceId());
			} finally {
				file.delete();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new ToolException("Error while deploying probe",e);
		}

//		context.done();
	}
	
	protected void setupToolOptions() {
		target = (TCPSocketLocator) context.getConfiguration().get("target");
		
		credential = (Credential) context.getConfiguration().get("credential");
		if (credential == null) {
			String username = (String) context.getConfiguration().get("username");
			String password = (String) context.getConfiguration().get("password");
			credential = new UsernameAndPassword(username, password);
		}

		probeConfigName = (String) context.getConfiguration().get("probeConfig");
		probeName = (String) context.getConfiguration().get("probeName");
	}
}
