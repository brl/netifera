package com.netifera.platform.net.cifs.tools;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Random;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbAuthException;

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

import com.netifera.platform.api.log.ILogger;
import com.netifera.platform.net.services.auth.TCPCredentialsVerifier;
import com.netifera.platform.net.services.credentials.UsernameAndPassword;
import com.netifera.platform.util.addresses.inet.TCPSocketAddress;

public class NTLMCredentialsVerifier extends TCPCredentialsVerifier {
	private String remoteName = "*SMBSERVER";
	private String localName = "";
	private boolean checkLocal = true;
	private boolean checkDomain = true;
	private ILogger logger;

	private Random random = new Random(System.currentTimeMillis());

	public NTLMCredentialsVerifier(TCPSocketAddress target, String remoteName, String localName, boolean checkDomain, boolean checkLocal, ILogger logger) {
		super(target);
		this.remoteName = remoteName;
		this.localName = localName;
		this.checkDomain = checkDomain;
		this.checkLocal = checkLocal;
		this.logger = logger;
	}

	@Override
	protected ChannelPipeline createPipeline() {
		ChannelPipeline pipeline = Channels.pipeline();

		pipeline.addLast("handler", new NTLMAuthChannelHandler());
		return pipeline;
	}

	@ChannelPipelineCoverage("one")
	class NTLMAuthChannelHandler extends SimpleChannelHandler {
		UsernameAndPassword credential = (UsernameAndPassword) nextCredentialOrNull();
		boolean sessionRequestSent, negotiateProtocolSent, sessionSetupSent;
		final short sessionKey = (short) (random.nextInt() & 0xFFFF);
		
		@Override
	    public void channelConnected(
	            ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
			if (credential == null) {
				e.getChannel().close();
			} else {
				if (target.getPort() != 445) {
					e.getChannel().write(netbiosSessionRequestPacket());
					sessionRequestSent = true;
				} else {
					e.getChannel().write(negotiateProtocolPacket(sessionKey));
					sessionRequestSent = true;
					negotiateProtocolSent = true;
				}
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
				e.getChannel().write(negotiateProtocolPacket(sessionKey));
				negotiateProtocolSent = true;
			} else if (!sessionSetupSent) {
				ByteBuffer dst = buffer.toByteBuffer();
				dst.order(ByteOrder.LITTLE_ENDIAN);
				int status = dst.getInt(9);
				if (status != SmbAuthException.NT_STATUS_OK) {
					throw new Exception("Bad response to SMB Protocol Negotiation, NT Status Code: "+String.format("0x%x",status));
				}

				// get the challenge
				byte[] challenge = new byte[8];
				for (int i=0; i<challenge.length; i++)
					challenge[i] = dst.get(73+i);

				// get workgroup and machine name
				byte[] workgroupBytes = new byte[16];
				byte[] machineNameBytes = new byte[16];

				//FIXME this ignores unicode 2nd byte
				int i=0;
				while ((dst.get(81 + i * 2) != 0) && (i < 16)) {
					workgroupBytes[i] = dst.get(81 + i * 2);
					i++;
				}

				int j=0;
				while ((dst.get(81 + (i + j + 1) * 2) != 0) && (j < 16)) {
					machineNameBytes[j] = dst.get(81 + (i + j + 1) * 2);
					j++;
				}

				String workgroup = new String(workgroupBytes);
				String machineName = new String(machineNameBytes);
				
				//FIXME should do this only once:
				
/*				InternetAddressEntity addressEntity = Activator.getInstance().getNetworkEntityFactory().createAddress(context.getRealm(), context.getSpaceId(), locator.getAddress());
				addressEntity.addName(machineName);
				addressEntity.update();
				
				HostEntity hostEntity = addressEntity.getHost();
				hostEntity.setAttribute("workgroup", workgroup);
//				hostEntity.setNamedAttribute("netbiosname", machineName);
				hostEntity.update();
*/				
				if (checkLocal)
					if (checkDomain)
						workgroup = "";
					else
						workgroup = "localhost";
				
				e.getChannel().write(sessionSetupPacket(sessionKey, credential, workgroup, challenge));
				sessionSetupSent = true;
			} else {
				ByteBuffer dst = buffer.toByteBuffer();
				dst.order(ByteOrder.LITTLE_ENDIAN);
				int status = dst.getInt(9);
				short action = dst.remaining() > 41 ? dst.getShort(41) : 0x0000;
				switch (status) {
				case SmbAuthException.NT_STATUS_ACCOUNT_LOCKED_OUT:
					logger.warning("Account Locked Out: "+credential);
					markBadUser(((UsernameAndPassword)credential).getUsernameString());
					break;
				case SmbAuthException.NT_STATUS_ACCOUNT_DISABLED:
					logger.warning("Account Disabled: "+credential);
					markBadUser(((UsernameAndPassword)credential).getUsernameString());
					break;
				case SmbAuthException.NT_STATUS_LOGON_FAILURE:
					// continue trying more passwords for this user
					break;
				case SmbAuthException.NT_STATUS_PASSWORD_EXPIRED:
					logger.warning("Expired Password: "+credential);
					markBadUser(((UsernameAndPassword)credential).getUsernameString());
					break;
				case SmbAuthException.NT_STATUS_PASSWORD_MUST_CHANGE:
					logger.warning("Change Password On Next Login: "+credential);
					break;
				case SmbAuthException.NT_STATUS_OK:
/*					if (action == 0x0001)
						logger.debug("Invalid account (anonymous connection): "+credential);
*/					break;
				default:
					logger.debug("Unknown NT Status Code for SMB Session Setup: "+String.format("0x%x", status));
				}

				if ((status == SmbAuthException.NT_STATUS_OK || status == SmbAuthException.NT_STATUS_PASSWORD_MUST_CHANGE) && (action != 0x0001))
					authenticationSucceeded(credential);
				else
					authenticationFailed(credential);

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

	private ChannelBuffer netbiosSessionRequestPacket() {
		ByteBuffer buffer = ByteBuffer.allocate(1024);
		buffer.put(new byte[] { (byte) 0x81, 0x00, 0x00, 0x48, 0x20 });
		buffer.put((getNetBIOSName(remoteName)+"CA").getBytes());
		buffer.put(new byte[] { 0x00, 0x20 });
		buffer.put((getNetBIOSName(localName)+"CA").getBytes());
		buffer.put((byte) 0);
		buffer.flip();
		return ChannelBuffers.wrappedBuffer(buffer);
	}

	private ChannelBuffer negotiateProtocolPacket(short sessionKey) {
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
		
		return ChannelBuffers.wrappedBuffer(packet);
	}

	private ChannelBuffer sessionSetupPacket(short sessionKey, UsernameAndPassword credential, String workgroup, byte[] challenge) {
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

		//XXX "" domain is ok? null would be substituted with smb.client.domain property
		NtlmPasswordAuthentication ntlm = new NtlmPasswordAuthentication("", username, password);
		buffer.put(ntlm.getAnsiHash(challenge));
		buffer.put(NtlmPasswordAuthentication.getNTLMResponse(password, challenge));
		
		buffer.put(username.getBytes());
		buffer.put((byte)0);
		buffer.put(workgroup.getBytes());
		buffer.put((byte)0);
		
		// native os = unix, native lan manager = samba
		buffer.put(new byte[] { 0x55, 0x6e, 0x69, 0x78, 0x00, 0x53, 0x61, 0x6d, 0x62, 0x61, 0x00 });
		
		buffer.flip();
		return ChannelBuffers.wrappedBuffer(buffer);
	}
}
