package com.netifera.platform.net.tools.bruteforce;

import java.io.IOException;

import org.jboss.netty.channel.Channel;
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
import org.jboss.netty.handler.codec.oneone.OneToOneDecoder;
import org.jboss.netty.handler.codec.string.StringDecoder;
import org.jboss.netty.handler.codec.string.StringEncoder;

import com.netifera.platform.net.services.auth.TCPCredentialsVerifier;
import com.netifera.platform.net.services.credentials.UsernameAndPassword;
import com.netifera.platform.util.locators.TCPSocketLocator;

public class FTPCredentialsVerifier extends TCPCredentialsVerifier {

	public FTPCredentialsVerifier(TCPSocketLocator locator) {
		super(locator);
	}

	@Override
	protected ChannelPipeline createPipeline() {
		ChannelPipeline pipeline = Channels.pipeline();

		pipeline.addLast("framer", new DelimiterBasedFrameDecoder(
				8192, Delimiters.lineDelimiter()));
		
		pipeline.addLast("decoder", new StringDecoder());
		pipeline.addLast("encoder", new StringEncoder());

		pipeline.addLast("resultDecoder", new FTPResponseDecoder());

		pipeline.addLast("handler", new FTPAuthChannelHandler());
		return pipeline;
	}

	@ChannelPipelineCoverage("all")
	class FTPResponseDecoder extends OneToOneDecoder {
		@Override
		protected Object decode(ChannelHandlerContext ctx,
				Channel channel, Object msg) throws Exception {
			if (!(msg instanceof String))
				return msg;
			String result = (String) msg;
			if (result.startsWith("220") || result.startsWith("500"))
				return null; // skip
			if (result.matches("^\\d\\d\\d.*"))
				return Integer.parseInt(result.substring(0,3));
			throw new IOException("Invalid FTP response: '"+result+"'");
		}
	}
	
	@ChannelPipelineCoverage("one")
	class FTPAuthChannelHandler extends SimpleChannelHandler {
		UsernameAndPassword credential = (UsernameAndPassword) nextCredentialOrNull();
		boolean userSent, passwordSent;
		
		@Override
	    public void channelConnected(
	            ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
			if (credential == null) {
				e.getChannel().close();
			} else if (!userSent) {
				e.getChannel().write("USER "+credential.getUsernameString()+"\r\n");
				userSent = true;
			}
			super.channelConnected(ctx, e);
	    }
	    
		@Override
		public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
				throws Exception {
			if (e.getMessage() instanceof Integer) {
				Integer result = (Integer) e.getMessage();
				if (!userSent) {
					// NOTHING
				} else if (!passwordSent) {
					if (result == 230) {
						authenticationSucceeded(new UsernameAndPassword(credential.getUsernameString(), ""));
						e.getChannel().close();
					} else {
						e.getChannel().write("PASS "+credential.getPasswordString()+"\r\n");
						passwordSent = true;
					}
				} else {
					if (result == 230) {
						authenticationSucceeded(credential);
						e.getChannel().close();
					} else {
						authenticationFailed(credential);
						credential = (UsernameAndPassword) nextCredentialOrNull();
						if (credential == null) {
							e.getChannel().close();
						} else {
							userSent = passwordSent = false;
							e.getChannel().write("USER "+credential.getUsernameString()+"\r\n");
							userSent = true;
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
