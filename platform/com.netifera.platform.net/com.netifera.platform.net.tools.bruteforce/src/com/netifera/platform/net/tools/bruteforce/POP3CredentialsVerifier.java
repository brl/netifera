package com.netifera.platform.net.tools.bruteforce;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.handler.codec.frame.DelimiterBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.Delimiters;
import org.jboss.netty.handler.codec.string.StringDecoder;
import org.jboss.netty.handler.codec.string.StringEncoder;

import com.netifera.platform.net.services.auth.TCPCredentialsVerifier;
import com.netifera.platform.net.services.credentials.UsernameAndPassword;
import com.netifera.platform.util.addresses.inet.TCPSocketAddress;

public class POP3CredentialsVerifier extends TCPCredentialsVerifier {

	public POP3CredentialsVerifier(TCPSocketAddress target) {
		super(target);
	}

	@Override
	protected ChannelPipeline createPipeline() {
		ChannelPipeline pipeline = Channels.pipeline();

		pipeline.addLast("framer", new DelimiterBasedFrameDecoder(
				8192, Delimiters.lineDelimiter()));
		
		pipeline.addLast("decoder", new StringDecoder());
		pipeline.addLast("encoder", new StringEncoder());

		pipeline.addLast("handler", new POP3AuthChannelHandler());
		return pipeline;
	}

	@ChannelPipelineCoverage("one")
	class POP3AuthChannelHandler extends SimpleChannelHandler {
		UsernameAndPassword credential = (UsernameAndPassword) nextCredentialOrNull();
		boolean userSent, passwordSent;
		
		@Override
	    public void channelConnected(
	            ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
			if (credential == null)
				e.getChannel().close();
			
			super.channelConnected(ctx, e);
	    }
	    
		@Override
		public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
				throws Exception {
			if (credential == null)
				return;
			if (e.getMessage() instanceof String) {
				String result = (String) e.getMessage();
				if (!userSent) {
					if (!result.startsWith("+OK"))
						return;
					e.getChannel().write("USER "+credential.getUsernameString()+"\r\n");
					userSent = true;
				} else if (!passwordSent) {
					if (!result.startsWith("+OK"))
						return;
					e.getChannel().write("PASS "+credential.getPasswordString()+"\r\n");
					passwordSent = true;
				} else {
					if (result.startsWith("+OK")) {
						authenticationSucceeded(credential);
						e.getChannel().close();
					} else {
						authenticationFailed(credential);
						credential = (UsernameAndPassword) nextCredentialOrNull();
						if (credential == null) {
							e.getChannel().close();
						} else {
							e.getChannel().write("USER "+credential.getUsernameString()+"\r\n");
							userSent = true;
							passwordSent = false;
						}
					}
				}
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
}
