package com.netifera.platform.host.filesystem.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;

import com.netifera.platform.api.tools.ITool;
import com.netifera.platform.api.tools.IToolContext;
import com.netifera.platform.api.tools.ToolException;
import com.netifera.platform.host.filesystem.IFileSystem;
import com.netifera.platform.host.filesystem.tools.internal.Activator;
import com.netifera.platform.util.HexaEncoding;
import com.netifera.platform.util.addresses.inet.InternetAddress;

public class Netstat implements ITool {
	private IToolContext context;
	
	private IFileSystem fileSystem;

	public void toolRun(IToolContext context) throws ToolException {
		this.context = context;

		context.setTitle("Netstat");
		setupToolOptions();

		try {
			process("/proc/net/udp", "UDP");
			process("/proc/net/udp6", "UDP");
			process("/proc/net/tcp", "TCP");
			process("/proc/net/tcp6", "TCP");
/*		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			context.warning("Interrupted");
*/		} finally {
			context.done();
		}
	}

	private void process(String path, String protocol) {
		InputStream stream = null;
		try {
			stream = fileSystem.getInputStream(path);
			BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
			String line = reader.readLine();
			line = reader.readLine();
			while (line != null) {
				line = line.trim();
				String[] parts = line.split(" ");
				String local = getSocketAddress(parts[1]);
				String remote = getSocketAddress(parts[2]);
				String state = getState(parts[3]);
				context.getLogger().info(protocol+" "+local+" <-> "+remote+" "+state);
				line = reader.readLine();
			}
		} catch (IOException e) {
			context.error("I/O error: " + e.getMessage());
		} finally {
			try {
				if (stream != null)
					stream.close();
			} catch (IOException e) {
			}
		}

	}
	
	private String getSocketAddress(String hexa) {
		String[] parts = hexa.split(":");
		return getAddress(parts[0])+":"+Integer.parseInt(parts[1], 16);
	}
	
	private InternetAddress getAddress(String hexa) {
		byte[] bytes = HexaEncoding.hex2bytes(hexa);
		byte[] reversed = new byte[bytes.length];
		for (int i=0; i<bytes.length; i++)
			reversed[reversed.length-1-i] = bytes[i];
		return InternetAddress.fromBytes(reversed);
	}
	
	private String getState(String hexa) {
		if (hexa.equals("01"))
			return "ESTABLISHED";
		if (hexa.equals("0A"))
			return "LISTEN";
		return hexa;
	}
	
	private void setupToolOptions() throws ToolException {
		try {
			fileSystem = (IFileSystem) Activator.getInstance().getServiceFactory().create(IFileSystem.class, new URI((String)context.getConfiguration().get("target")));
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}
}
