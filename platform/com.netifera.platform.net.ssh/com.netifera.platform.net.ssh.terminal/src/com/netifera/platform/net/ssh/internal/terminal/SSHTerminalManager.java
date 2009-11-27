package com.netifera.platform.net.ssh.internal.terminal;

import java.io.IOException;
import java.net.URI;

import com.netifera.platform.host.terminal.ITerminal;
import com.netifera.platform.host.terminal.ITerminalService;
import com.netifera.platform.host.terminal.ITerminalOutputHandler;
import com.netifera.platform.net.services.credentials.Credential;
import com.netifera.platform.net.services.credentials.UsernameAndPassword;
import com.netifera.platform.net.services.ssh.SSH;
import com.netifera.platform.util.addresses.inet.InternetAddress;
import com.netifera.platform.util.locators.TCPSocketLocator;
import com.trilead.ssh2.ChannelCondition;
import com.trilead.ssh2.Connection;
import com.trilead.ssh2.Session;

public class SSHTerminalManager implements ITerminalService {
	
	private SSH ssh;
	private Credential credential;
	private Connection connection;

	public SSHTerminalManager(URI url) {
		InternetAddress address = InternetAddress.fromString(url.getHost());
		TCPSocketLocator locator = new TCPSocketLocator(address, url.getPort());
		this.ssh = new SSH(locator);

		String[] userInfo = url.getUserInfo().split(":");
		String username = userInfo[0];
		String password = userInfo.length > 1 ? userInfo[1] : "";
		this.credential = new UsernameAndPassword(username, password);
	}
	
	public SSHTerminalManager(SSH ssh, UsernameAndPassword credential) {
		this.ssh = ssh;
		this.credential = credential;
	}

	public ITerminal openTerminal(String command, final ITerminalOutputHandler outputHandler) throws IOException {
		connection = ssh.createConnection(credential);

        final Session session = connection.openSession();
        session.requestPTY("ansi", 0, 0, 0, 0, null);
        session.startShell();

        final String ptyName = session.toString();
        
		ITerminal terminal = new ITerminal() {
			public void close() {
				session.close();
			}

			public String getName() {
				return ptyName;
			}

			public void sendInput(byte[] data) {
				try {
					session.getStdin().write(data, 0, data.length);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			public void setSize(int width, int height) {
//				session.setPtySize(newWidth, newHeight, 8*newWidth, 8*newHeight);
			}
		};

        Thread outputThread = new Thread(new Runnable() {
			public void run() {
				byte buffer[] = new byte[4096];
				
				while (true) {
					int conditions = session.waitForCondition(0, ChannelCondition.STDOUT_DATA | ChannelCondition.STDERR_DATA | ChannelCondition.EOF);

					if ((conditions & ChannelCondition.STDOUT_DATA) != 0) {
						try {
							int count = session.getStdout().read(buffer);
							outputHandler.terminalOutput(ptyName, buffer, count);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}

					if ((conditions & ChannelCondition.STDERR_DATA) != 0) {
						try {
							int count = session.getStderr().read(buffer);
							outputHandler.terminalOutput(ptyName, buffer, count);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}

					/* Here we do not need to check separately for CLOSED, since CLOSED implies EOF */
	
					if ((conditions & ChannelCondition.EOF) != 0) {
						outputHandler.terminalClosed(ptyName);
						return;
					}

//					throw new IllegalStateException("Unexpected condition result (" + conditions + ")");
				}
			}
        });

        outputThread.start();
        
        return terminal;
	}

	public void disconnect() {
		connection.close();
	}
}
