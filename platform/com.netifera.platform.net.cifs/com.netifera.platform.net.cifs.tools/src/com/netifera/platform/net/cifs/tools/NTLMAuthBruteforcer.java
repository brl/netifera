package com.netifera.platform.net.cifs.tools;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbAuthException;

import com.netifera.platform.api.tools.ToolException;
import com.netifera.platform.net.cifs.internal.tools.Activator;
import com.netifera.platform.net.model.UserEntity;
import com.netifera.platform.net.services.auth.CredentialsVerifier;
import com.netifera.platform.net.services.auth.TCPCredentialsVerifier;
import com.netifera.platform.net.services.credentials.Credential;
import com.netifera.platform.net.services.credentials.UsernameAndPassword;
import com.netifera.platform.net.sockets.CompletionHandler;
import com.netifera.platform.net.sockets.TCPChannel;
import com.netifera.platform.net.tools.bruteforce.UsernameAndPasswordBruteforcer;
import com.netifera.platform.util.locators.TCPSocketLocator;

public class NTLMAuthBruteforcer extends UsernameAndPasswordBruteforcer {
	private TCPSocketLocator target;
	private String remoteName = "*SMBSERVER";
	private String localName = "";
	
	@Override
	protected void setupToolOptions() throws ToolException {
		target = (TCPSocketLocator) context.getConfiguration().get("target");
		context.setTitle("Bruteforce LM authentication on SMB @ "+target);
		
		if (context.getConfiguration().get("remoteName") != null)
			remoteName = (String) context.getConfiguration().get("remoteName");
		if (context.getConfiguration().get("localName") != null)
			localName = (String) context.getConfiguration().get("localName");
		
		super.setupToolOptions();
	}
	
	@Override
	public void authenticationSucceeded(Credential credential) {
		UsernameAndPassword up = (UsernameAndPassword) credential;
		Activator.getInstance().getNetworkEntityFactory().createUsernameAndPassword(realm, context.getSpaceId(), target, up.getUsernameString(), up.getPasswordString());
		
		UserEntity user = Activator.getInstance().getNetworkEntityFactory().createUser(realm, context.getSpaceId(), target.getAddress(), up.getUsernameString());
		user.setPassword(up.getPasswordString());
		user.update();
		
		super.authenticationSucceeded(credential);
	}

	private String getNetBIOSName(String hostName) {
		//  Build the name string with the name type, make sure that the host
		//  name is uppercase.
		StringBuffer hName = new StringBuffer(hostName.toUpperCase());

		if (hName.length() > 15)
			hName.setLength(15);

		//  Space pad the name then add the NetBIOS name type
		while (hName.length () < 15)
			hName.append(' ');
//		hName.append("XXX"); // Name type
		
		//  Convert the NetBIOS name string to the RFC NetBIOS name format
		String convstr = new String("ABCDEFGHIJKLMNOP");
		StringBuffer nameBuf = new StringBuffer(32);
		
		int idx = 0;
		while (idx < hName.length()) {
			//  Get the current character from the host name string
			char ch = hName.charAt(idx++);
			if (ch == ' ') {
				//  Append an encoded <SPACE> character
				nameBuf.append("CA");
			} else {
				//  Append octet for the current character
				nameBuf.append(convstr.charAt((int) ch / 16));
				nameBuf.append(convstr.charAt((int) ch % 16));
			}
		}
		return nameBuf.toString ();
	}
	
	@Override
	public CredentialsVerifier createCredentialsVerifier() {
		TCPCredentialsVerifier verifier = new TCPCredentialsVerifier(target) {
			
			private Random random = new Random(System.currentTimeMillis());
			
			@Override
			protected void authenticate(final TCPChannel channel, final Credential credential,
					final long timeout, final TimeUnit unit,
					final CompletionHandler<Boolean, Credential> handler) {

				if (target.getPort() != 445) {
					sessionRequest(channel, credential, timeout, unit, handler);
				} else {
					login(channel, credential, timeout, unit, handler);
				}
			}
			
			private void sessionRequest(final TCPChannel channel, final Credential credential,
					final long timeout, final TimeUnit unit,
					final CompletionHandler<Boolean, Credential> handler) {
				channel.write(netbiosSessionRequestPacket(), timeout, unit, null, new CompletionHandler<Integer,Void>() {
					public void completed(Integer result, Void attachment) {
						final ByteBuffer dst = ByteBuffer.allocate(2048);
						channel.read(dst, timeout, unit, attachment, new CompletionHandler<Integer,Void>() {
							public void completed(Integer result, Void attachment) {
								dst.flip();
								int code = dst.get() & 0xFF;
								if (code != 0x82) {
									if (code == 0x83) { // insuficient resources
										context.warning("Server replied 'insufficient resources'");
										handler.failed(new Exception("Insuficient resources"), credential);
									} else {
										context.error("Unknown response code to Session Request: "+String.format("0x%x",code));
										cancel();
									}
//									failed
									return;
								}
								login(channel, credential, timeout, unit, handler);
							}
							public void cancelled(Void attachment) {
								handler.cancelled(credential);
							}
							public void failed(Throwable exc, Void attachment) {
								handler.failed(exc, credential);
							}
						});
					}
					public void cancelled(Void attachment) {
						handler.cancelled(credential);
					}
					public void failed(Throwable exc, Void attachment) {
						exc.printStackTrace();
						handler.failed(exc, credential);
					}
				});
			}
			
			private void login(final TCPChannel channel, final Credential credential,
					final long timeout, final TimeUnit unit,
					final CompletionHandler<Boolean, Credential> handler) {
				final short sessionKey = (short) (random.nextInt() & 0xFFFF);
				channel.write(negotiateProtocolPacket(sessionKey), timeout, unit, null, new CompletionHandler<Integer,Void>() {
					public void completed(Integer result, Void attachment) {
//						dst.clear();
						final ByteBuffer dst = ByteBuffer.allocate(2048);
						channel.read(dst, timeout, unit, attachment, new CompletionHandler<Integer,Void>() {
							public void completed(Integer result, Void attachment) {
								dst.flip();
/*												int response = dst.get(9) & 0xFF;
								if (response != 0x00) {
									context.error("Bad response to Protocol Negotiation: "+String.format("0x%x",response));
									cancel();
//									failed
									return;
								}
*/
								// get the challenge
								byte[] challenge = new byte[8];
								for (int i=0; i<challenge.length; i++)
									challenge[i] = dst.get(73+i);

								// get workgroup and machine name
								byte[] workgroup = new byte[16];
								byte[] machineName = new byte[16];

								//FIXME this ignored unicode 2nd byte
								int i=0;
								while ((dst.get(81 + i * 2) != 0) && (i < 16)) {
									workgroup[i] = dst.get(81 + i * 2);
									i++;
								}

								int j=0;
								while ((dst.get(81 + (i + j + 1) * 2) != 0) && (j < 16)) {
									machineName[j] = dst.get(81 + (i + j + 1) * 2);
									j++;
								}

								channel.write(sessionSetupPacket(sessionKey, (UsernameAndPassword)credential, new String(workgroup), challenge), timeout, unit, null, new CompletionHandler<Integer,Void>() {
									public void completed(Integer result, Void attachment) {
										dst.clear();
										channel.read(dst, timeout, unit, attachment, new CompletionHandler<Integer,Void>() {
											public void completed(Integer result, Void attachment) {
												dst.flip();
												dst.order(ByteOrder.LITTLE_ENDIAN);
												int status = dst.getInt(9);
												switch (status) {
												case SmbAuthException.NT_STATUS_ACCOUNT_LOCKED_OUT:
													context.warning("Account Locked Out: "+credential);
													break;
												case SmbAuthException.NT_STATUS_ACCOUNT_DISABLED:
													context.warning("Account Disabled: "+credential);
													break;
												case SmbAuthException.NT_STATUS_OK:
													break;
												default:
													context.debug("Unknown NT Status Code: "+String.format("%x", status));
												}
												
												handler.completed(status == SmbAuthException.NT_STATUS_OK, credential);
												try {
													channel.close();
												} catch (IOException e) {
													// TODO Auto-generated catch block
													e.printStackTrace();
												}
											}
											public void cancelled(Void attachment) {
												handler.cancelled(credential);
											}
											public void failed(Throwable exc, Void attachment) {
												handler.failed(exc, credential);
											}
										});
									}
									public void cancelled(Void attachment) {
										handler.cancelled(credential);
									}
									public void failed(Throwable exc, Void attachment) {
										handler.failed(exc, credential);
									}
								});
							}
							public void cancelled(Void attachment) {
								handler.cancelled(credential);
							}
							public void failed(Throwable exc, Void attachment) {
								handler.failed(exc, credential);
							}
						});
					}
					public void cancelled(Void attachment) {
						handler.cancelled(credential);
					}
					public void failed(Throwable exc, Void attachment) {
						handler.failed(exc, credential);
					}
				});

			}
		};
		
		verifier.setMaximumConnections((Integer) context.getConfiguration().get("maximumConnections"));
		return verifier;
	}
	
	private ByteBuffer netbiosSessionRequestPacket() {
		ByteBuffer buffer = ByteBuffer.allocate(1024);
		buffer.put(new byte[] { (byte) 0x81, 0x00, 0x00, 0x48, 0x20 });
		buffer.put((getNetBIOSName(remoteName)+"CA").getBytes());
		buffer.put(new byte[] { 0x00, 0x20 });
		buffer.put((getNetBIOSName(localName)+"CA").getBytes());
		buffer.put((byte) 0);
		buffer.flip();
		return buffer;
	}

	private ByteBuffer negotiateProtocolPacket(short sessionKey) {
		byte[] packet = {
			0x00, 0x00, 0x00, (byte) 0xa4, (byte) 0xff, 0x53, 0x4d, 0x42,
			0x72, 0x00, 0x00, 0x00, 0x00, 0x08, 0x01, 0x40,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x3c, 0x7d,
			0x00, 0x00, 0x01, 0x00, 0x00, (byte) 0x81, 0x00, 0x02,
			0x50, 0x43, 0x20, 0x4e, 0x45, 0x54, 0x57, 0x4f,
			0x52, 0x4b, 0x20, 0x50, 0x52, 0x4f, 0x47, 0x52,
			0x41, 0x4d, 0x20, 0x31, 0x2e, 0x30, 0x00, 0x02,
			0x4d, 0x49, 0x43, 0x52, 0x4f, 0x53, 0x4f, 0x46,
			0x54, 0x20, 0x4e, 0x45, 0x54, 0x57, 0x4f, 0x52,
			0x4b, 0x53, 0x20, 0x31, 0x2e, 0x30, 0x33, 0x00,
			0x02, 0x4d, 0x49, 0x43, 0x52, 0x4f, 0x53, 0x4f,
			0x46, 0x54, 0x20, 0x4e, 0x45, 0x54, 0x57, 0x4f,
			0x52, 0x4b, 0x53, 0x20, 0x33, 0x2e, 0x30, 0x00,
			0x02, 0x4c, 0x41, 0x4e, 0x4d, 0x41, 0x4e, 0x31,
			0x2e, 0x30, 0x00, 0x02, 0x4c, 0x4d, 0x31, 0x2e,
			0x32, 0x58, 0x30, 0x30, 0x32, 0x00, 0x02, 0x53,
			0x61, 0x6d, 0x62, 0x61, 0x00, 0x02, 0x4e, 0x54,
			0x20, 0x4c, 0x41, 0x4e, 0x4d, 0x41, 0x4e, 0x20,
			0x31, 0x2e, 0x30, 0x00, 0x02, 0x4e, 0x54, 0x20,
			0x4c, 0x4d, 0x20, 0x30, 0x2e, 0x31, 0x32, 0x00
		};
		
		packet[30] = (byte) (sessionKey & 0xFF);
		packet[31] = (byte) ((sessionKey >> 8) & 0xFF);
		
		packet[32] = (byte) 0xcd; packet[33] = (byte) 0xef; // user id
		
		return ByteBuffer.wrap(packet);
	}

	private ByteBuffer sessionSetupPacket(short sessionKey, UsernameAndPassword credential, String workgroup, byte[] challenge) {
		String username = credential.getUsernameString();
		String password = credential.getPasswordString();

		ByteBuffer buffer = ByteBuffer.allocate(2048);

		buffer.put((byte) 0); // session message
		
		// header length
		buffer.order(ByteOrder.BIG_ENDIAN);
		buffer.put((byte) 0);
		buffer.putShort((short)(username.length() + workgroup.length() + 0x7A));

		byte[] header = new byte[] {
			/*0x00, 0x00, 0x00, (byte) 0x85,*/ (byte) 0xff, 0x53, 0x4d,
			0x42, 0x73, 0x00, 0x00, 0x00, 0x00, 0x08, 0x01,
			0x40, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x3c,
			0x7d, 0x00, 0x00, 0x01, 0x00, 0x0d, (byte) 0xff, 0x00,
			0x00, 0x00, (byte) 0xff, (byte) 0xff, 0x02, 0x00, 0x3c, 0x7d,
			0x00, 0x00, 0x00, 0x00, 0x18, 0x00, 0x18, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x50, 0x00, 0x00, 0x00 };

		header[30] = (byte) (sessionKey & 0xFF);
		header[31] = (byte) ((sessionKey >> 8) & 0xFF);
		
		buffer.put(header);

		// data length
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		buffer.putShort((short) (0x1f + workgroup.length() + username.length()));
		
		NtlmPasswordAuthentication ntlm = new NtlmPasswordAuthentication(username+":"+password);
		buffer.put(ntlm.getAnsiHash(challenge));
		buffer.put(NtlmPasswordAuthentication.getNTLMResponse(password, challenge));
		
		buffer.put(username.getBytes());
		buffer.put((byte)0);
		buffer.put(workgroup.getBytes());
		buffer.put((byte)0);
		
		// native os = unix, native lan manager = samba
		buffer.put(new byte[] { 0x55, 0x6e, 0x69, 0x78, 0x00, 0x53, 0x61, 0x6d, 0x62, 0x61, 0x00 });
		
		buffer.flip();
		return buffer;
	}
}
