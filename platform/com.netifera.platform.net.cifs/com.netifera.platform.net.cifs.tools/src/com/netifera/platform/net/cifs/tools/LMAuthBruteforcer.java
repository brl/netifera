package com.netifera.platform.net.cifs.tools;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.TimeUnit;

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

public class LMAuthBruteforcer extends UsernameAndPasswordBruteforcer {
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
//		user.setHash("LM", hash(credential));
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
			@Override
			protected void authenticate(final TCPChannel channel, final Credential credential,
					final long timeout, final TimeUnit unit,
					final CompletionHandler<Boolean, Credential> handler) {
				
				channel.write(sessionRequestPacket(), timeout, unit, null, new CompletionHandler<Integer,Void>() {
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
								channel.write(negotiateProtocolPacket(), timeout, unit, null, new CompletionHandler<Integer,Void>() {
									public void completed(Integer result, Void attachment) {
										dst.clear();
										channel.read(dst, timeout, unit, attachment, new CompletionHandler<Integer,Void>() {
											public void completed(Integer result, Void attachment) {
												dst.flip();
												int response = dst.get(9) & 0xFF;
												if (response != 0x00) {
													context.error("Bad response to Protocol Negotiation: "+String.format("0x%x",response));
													cancel();
//													failed
													return;
												}
												channel.write(sessionSetupPacket((UsernameAndPassword)credential), timeout, unit, null, new CompletionHandler<Integer,Void>() {
													public void completed(Integer result, Void attachment) {
														dst.clear();
														channel.read(dst, timeout, unit, attachment, new CompletionHandler<Integer,Void>() {
															public void completed(Integer result, Void attachment) {
																dst.flip();
																handler.completed((dst.get(4) & 0xFF) == 0xFF && (dst.get(9) & 0xFF) == 0x00, credential);
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
		};
		
		verifier.setMaximumConnections((Integer) context.getConfiguration().get("maximumConnections"));
		return verifier;
	}
	
	private ByteBuffer sessionRequestPacket() {
		ByteBuffer buffer = ByteBuffer.allocate(1024);
		buffer.put(new byte[] { (byte) 0x81, 0x00, 0x00, 0x44, 0x20 });
		buffer.put((getNetBIOSName(remoteName)+"CA").getBytes());
		buffer.put(new byte[] { 0x00, 0x20 });
		buffer.put((getNetBIOSName(localName)+"CA").getBytes());
		buffer.put((byte) 0);
		buffer.flip();
		return buffer;
	}
	
	private ByteBuffer negotiateProtocolPacket() {
		byte[] request = { 0x00, 0x00,
			    0x00, (byte)0x89, (byte)0xFF, 0x53, 0x4D, 0x42, 0x72, 0x00,
			    0x00, 0x00, 0x00, 0x18, 0x01, 0x20, 0x00, 0x00,
			    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
			    0x00, 0x00, 0x00, 0x00, 0x00, 0x28, 0x00, 0x00,
			    0x00, 0x00, 0x00, 0x66, 0x00, 0x02, 0x50, 0x43,
			    0x20, 0x4E, 0x45, 0x54, 0x57, 0x4F, 0x52, 0x4B,
			    0x20, 0x50, 0x52, 0x4F, 0x47, 0x52, 0x41, 0x4D,
			    0x20, 0x31, 0x2E, 0x30, 0x00, 0x02, 0x4D, 0x49,
			    0x43, 0x52, 0x4F, 0x53, 0x4F, 0x46, 0x54, 0x20,
			    0x4E, 0x45, 0x54, 0x57, 0x4F, 0x52, 0x4B, 0x53,
			    0x20, 0x31, 0x2E, 0x30, 0x33, 0x00, 0x02, 0x4D,
			    0x49, 0x43, 0x52, 0x4F, 0x53, 0x4F, 0x46, 0x54,
			    0x20, 0x4E, 0x45, 0x54, 0x57, 0x4F, 0x52, 0x4B,
			    0x53, 0x20, 0x33, 0x2e, 0x30, 0x00, 0x02, 0x4c,
			    0x41, 0x4e, 0x4d, 0x41, 0x4e, 0x31, 0x2e, 0x30,
			    0x00, 0x02, 0x4c, 0x4d, 0x31, 0x2e, 0x32, 0x58,
			    0x30, 0x30, 0x32, 0x00, 0x02, 0x53, 0x61, 0x6d,
			    0x62, 0x61, 0x00
			};

		return ByteBuffer.wrap(request);
	}
	
	private ByteBuffer sessionSetupPacket(UsernameAndPassword credential) {
		String username = credential.getUsernameString();
		String password = credential.getPasswordString();

		ByteBuffer buffer = ByteBuffer.allocate(2048);
		buffer.order(ByteOrder.BIG_ENDIAN);
		buffer.putShort((short)0);
		buffer.putShort((short)(username.length()+password.length()+57));
		buffer.put(new byte[] { (byte)0xFF, 0x53, 0x4D, 0x42, 0x73, 0x00,
				    0x00, 0x00, 0x00, 0x18, 0x01, 0x20, 0x00, 0x00,
				    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
				    0x00, 0x00, 0x00, 0x00, 0x00, 0x28, 0x00, 0x00,
				    0x00, 0x00, 0x0A, (byte)0xFF, 0x00, 0x00, 0x00, 0x04,
				    0x11, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
				    0x00 });
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		buffer.putShort((short)(password.length()+1));
		buffer.putInt(0);
		buffer.putShort((short)(username.length()+password.length()+2));
		buffer.put(password.getBytes());
		buffer.put((byte)0);
		buffer.put(username.getBytes());
		buffer.put((byte)0);
		buffer.flip();
		return buffer;
	}
}
