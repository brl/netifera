package com.netifera.platform.net.cifs.tools;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;

import com.netifera.platform.net.services.auth.TCPCredentialsVerifier;
import com.netifera.platform.net.services.credentials.UsernameAndPassword;
import com.netifera.platform.util.locators.TCPSocketLocator;

public class LMCredentialsVerifier extends TCPCredentialsVerifier {
	private String remoteName = "*SMBSERVER";
	private String localName = "";

	public LMCredentialsVerifier(TCPSocketLocator locator, String remoteName, String localName) {
		super(locator);
		this.remoteName = remoteName;
		this.localName = localName;
	}

	@Override
	protected ChannelPipeline createPipeline() {
		ChannelPipeline pipeline = Channels.pipeline();

		pipeline.addLast("handler", new LMAuthChannelHandler());
		return pipeline;
	}

	@ChannelPipelineCoverage("one")
	class LMAuthChannelHandler extends SimpleChannelHandler {
		UsernameAndPassword credential = (UsernameAndPassword) nextCredentialOrNull();
		boolean sessionRequestSent, negotiateProtocolSent, sessionSetupSent;
		
		@Override
	    public void channelConnected(
	            ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
			if (credential == null) {
				e.getChannel().close();
			} else {
				e.getChannel().write(sessionRequestPacket());
				sessionRequestSent = true;
			}
			super.channelConnected(ctx, e);
	    }
	    
		@Override
		public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
				throws Exception {
			ChannelBuffer buffer = (ChannelBuffer) e.getMessage();
			if (!sessionRequestSent) {
				// NOTHING
			} else if (!negotiateProtocolSent) {
				int code = buffer.getByte(0) & 0xFF;
				if (code != 0x82) {
					if (code == 0x83) { // insuficient resources
//						logger.warning("Server replied 'insufficient resources'");
						throw new Exception("Server replied 'insuficient resources'");
					}
					throw new Exception("Unknown response code to Session Request: "+String.format("0x%x",code));
				}
				e.getChannel().write(negotiateProtocolPacket());
				negotiateProtocolSent = true;
			} else if (!sessionSetupSent) {
				int response = buffer.getByte(9) & 0xFF;
				if (response != 0x00) {
					throw new Exception("Bad response to SMB Protocol Negotiation: "+String.format("0x%x",response));
				}
				e.getChannel().write(sessionSetupPacket(credential));
				sessionSetupSent = true;
			} else {
				if ((buffer.getByte(4) & 0xFF) == 0xFF && (buffer.getByte(9) & 0xFF) == 0x00) {
					authenticationSucceeded(credential);
				} else {
					authenticationFailed(credential);
				}
				e.getChannel().close();
			}
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
				throws Exception {
			e.getCause().printStackTrace();
			e.getChannel().close();
			if (credential != null) {
				authenticationError(credential, e.getCause());
			}
		}
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

	private ChannelBuffer sessionRequestPacket() {
		ByteBuffer buffer = ByteBuffer.allocate(1024);
		buffer.put(new byte[] { (byte) 0x81, 0x00, 0x00, 0x44, 0x20 });
		buffer.put((getNetBIOSName(remoteName)+"CA").getBytes());
		buffer.put(new byte[] { 0x00, 0x20 });
		buffer.put((getNetBIOSName(localName)+"CA").getBytes());
		buffer.put((byte) 0);
		buffer.flip();
		return ChannelBuffers.wrappedBuffer(buffer);
	}
	
	private ChannelBuffer negotiateProtocolPacket() {
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

		return ChannelBuffers.wrappedBuffer(request);
	}
	
	private ChannelBuffer sessionSetupPacket(UsernameAndPassword credential) {
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
		return ChannelBuffers.wrappedBuffer(buffer);
	}
}
