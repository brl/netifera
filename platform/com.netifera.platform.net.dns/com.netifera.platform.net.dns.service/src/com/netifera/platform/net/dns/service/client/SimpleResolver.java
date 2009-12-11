package com.netifera.platform.net.dns.service.client;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.jboss.netty.bootstrap.ConnectionlessBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.FixedReceiveBufferSizePredictorFactory;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.socket.DatagramChannel;
import org.jboss.netty.channel.socket.DatagramChannelFactory;
import org.jboss.netty.handler.timeout.IdleState;
import org.jboss.netty.handler.timeout.IdleStateHandler;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timer;
import org.xbill.DNS.Flags;
import org.xbill.DNS.Header;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.OPTRecord;
import org.xbill.DNS.Opcode;
import org.xbill.DNS.Options;
import org.xbill.DNS.Record;
import org.xbill.DNS.Resolver;
import org.xbill.DNS.ResolverListener;
import org.xbill.DNS.Section;
import org.xbill.DNS.TSIG;
import org.xbill.DNS.Type;
import org.xbill.DNS.WireParseException;
import org.xbill.DNS.ZoneTransferException;
import org.xbill.DNS.ZoneTransferIn;

import com.netifera.platform.api.log.ILogger;
import com.netifera.platform.util.locators.UDPSocketLocator;


public class SimpleResolver implements Resolver {
	private final UDPSocketLocator locator;
	private DatagramChannel channel;
	private Timer timer;

	public static final int TIMER_MSECS = 500;
	
	private ILogger logger;

	/** The default EDNS payload size */
	public static final int DEFAULT_EDNS_PAYLOADSIZE = 1280;

	private boolean /*useTCP,*/ ignoreTruncation;
	private OPTRecord queryOPT;
	private TSIG tsig;
	private int timeoutValue = 5 * 1000; // 5 seconds too short?
//	private int retries = 3;
	private static final short DEFAULT_UDPSIZE = 512;

	private Map<Integer,ResponseContext> contexts = new HashMap<Integer,ResponseContext>();

	class ResponseContext {
		final ResolverListener listener;
		final Message query;
		final long deadline;
//		long nextTryTime;
		
		ResponseContext(Message query, ResolverListener listener, long deadline) {
			this.listener = listener;
			this.deadline = deadline;
			this.query = query;
		}
	}
	
	class LazyResponse implements ResolverListener {
		Exception exception;
		Message message;
		
		public synchronized void handleException(Object id, Exception e) {
			this.exception = e;
			notifyAll();
		}
		
		public synchronized void receiveMessage(Object id, Message m) {
			this.message = m;
			notifyAll();
		}
	}
	
	public SimpleResolver(UDPSocketLocator locator, DatagramChannelFactory channelFactory) {
		this.locator = locator;
		
		ConnectionlessBootstrap bootstrap = new ConnectionlessBootstrap(channelFactory);

		// Configure the pipeline.
		ChannelPipeline pipeline = bootstrap.getPipeline();
		pipeline.addLast("timeout", createChannelIdleHandler());
		pipeline.addLast("handler", new DNSResponseHandler());
		
		// Allow packets as large as up to 4096 bytes (default is 768).
		// You could increase or decrease this value to avoid truncated packets.
		bootstrap.setOption(
				"receiveBufferSizePredictorFactory",
				new FixedReceiveBufferSizePredictorFactory(4096));

		try {
			channel = (DatagramChannel) bootstrap.connect(locator.toInetSocketAddress()).await().getChannel();
		} catch (Exception e) {
			error("Error while opening UDP connection to DNS server at "+locator, e);
			e.printStackTrace();
		}
	}

	public void setLogger(ILogger logger) {
		this.logger = logger;		
	}
	
	private void error(String message, Throwable exception) {
		if (logger != null) {
			logger.error(message, exception);
		} else {
			//exception.printStackTrace(System.err);
		}
	}

	private ChannelHandler createChannelIdleHandler() {
		if (timer == null)
			timer = new HashedWheelTimer();
		return new IdleStateHandler(timer, TIMER_MSECS, 0, 0, TimeUnit.MILLISECONDS) {
			@Override
		    protected void channelIdle(ChannelHandlerContext ctx, IdleState state, long lastActivityTimeMillis) throws Exception {
		    	checkTimeOut();
		    }
		};
	}
	
	@ChannelPipelineCoverage("one")
	class DNSResponseHandler extends SimpleChannelUpstreamHandler {
		@Override
		public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
				throws Exception {
			ChannelBuffer buffer = (ChannelBuffer) e.getMessage();
			byte[] in = new byte[buffer.readableBytes()]; //XXX do we really need to allocate a new array?
			buffer.readBytes(in);
			try {
				handleResponse(in);
			} catch (WireParseException ex) {
				error("Parsing exception while processing response", ex);
			} catch (Exception ex) {
				ex.printStackTrace();
				throw ex;
			}
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
				throws Exception {
			e.getCause().printStackTrace();
//			e.getChannel().close();
			checkTimeOut();
		}
	}
	private void handleResponse(byte[] in) throws WireParseException {
		/*
		 * Check that the response is long enough.
		 */
		if (in.length < Header.LENGTH) {
			throw new WireParseException("Invalid DNS header - too short ("+in.length+")");
		}
		/*
		 * Check that the response ID matches the query ID. We want to check
		 * this before actually parsing the message, so that if there's a
		 * malformed response that's not ours, it doesn't confuse us.
		 */
		Integer id = ((in[0] & 0xFF) << 8) + (in[1] & 0xFF);
		ResponseContext context;
		synchronized(this) {
			context = contexts.remove(id);
		}
		if (context == null) {
//		either a bad response or more likely a late or duplicated response
		} else {
			try {
				Message response = parseMessage(in);
				//TODO
//				System.out.println("Question: "+response.getQuestion());
//				System.out.println("Response: "+response);
				verifyTSIG(context.query, response, in, tsig);
/*				if (!tcp && !ignoreTruncation && response.getHeader().getFlag(Flags.TC)) {
					tcp = true;
					continue;
				}
*/
				context.listener.receiveMessage(id, response);
			} catch(Exception e) {
				context.listener.handleException(id, e);
				error("Exception while handling response", e);
			}
		}
	}

	private void handleException(int id, Exception e) {
		ResponseContext context;
		synchronized(this) {
			context = contexts.remove(id);
		}
		if (context != null)
			context.listener.handleException(id, e);
	}
	
	@Deprecated
	public void setTCP(boolean flag) {
//		this.useTCP = flag;
	}

	public void setIgnoreTruncation(boolean flag) {
		this.ignoreTruncation = flag;
	}

	@SuppressWarnings("unchecked")
	public void setEDNS(int level, int payloadSize, int flags, List options) {
		if (level != 0 && level != -1)
			throw new IllegalArgumentException("invalid EDNS level - "
					+ "must be 0 or -1");
		if (payloadSize == 0)
			payloadSize = DEFAULT_EDNS_PAYLOADSIZE;
		queryOPT = new OPTRecord(payloadSize, 0, level, flags, options);
	}

	public void setEDNS(int level) {
		setEDNS(level, 0, 0, null);
	}

	public void setTSIGKey(TSIG key) {
		tsig = key;
	}

	TSIG getTSIGKey() {
		return tsig;
	}

	public void setTimeout(int secs, int msecs) {
		timeoutValue = secs * 1000 + msecs;
//		System.err.println("setting timeout msecs "+timeoutValue);
	}

	public void setTimeout(int secs) {
		setTimeout(secs, 0);
	}

	long getTimeout() {
		return timeoutValue;
	}

	private Message parseMessage(byte[] b) throws WireParseException {
		try {
			return (new Message(b));
		} catch (IOException e) {
			if (Options.check("verbose"))
				e.printStackTrace();
			if (!(e instanceof WireParseException))
				e = new WireParseException("Error parsing message");
			throw (WireParseException) e;
		}
	}

	private void verifyTSIG(Message query, Message response, byte[] b, TSIG tsig) {
		if (tsig == null)
			return;
		int error = tsig.verify(response, b, query.getTSIG());
//		if (Options.check("verbose"))
//			System.err.println("TSIG verify: " + Rcode.string(error));*/
	}

	private void applyEDNS(Message query) {
		if (queryOPT == null || query.getOPT() != null)
			return;
		query.addRecord(queryOPT, Section.ADDITIONAL);
	}

/*	private int maxUDPSize(Message query) {
		OPTRecord opt = query.getOPT();
		return opt == null ? DEFAULT_UDPSIZE : opt.getPayloadSize();
	}
*/
	/**
	 * Sends a message to a single server and waits for a response. No checking
	 * is done to ensure that the response is associated with the query.
	 * 
	 * @param query
	 *            The query to send.
	 * @return The response.
	 * @throws IOException
	 *             An error occurred while sending or receiving.
	 */
	public Message send(Message query) throws IOException {
		if (Options.check("verbose")) {
			System.err.println("Sending "+query);
/*			System.err.println("Sending to "
					+ address.getAddress().getHostAddress() + ":"
					+ address.getPort());
*/		}
		
		if (query.getHeader().getOpcode() == Opcode.QUERY) {
			Record question = query.getQuestion();
			if (question != null && question.getType() == Type.AXFR)
				return sendAXFR(query);
		}

		LazyResponse lazyResponse = new LazyResponse();
		sendAsync(query, lazyResponse);
		try {
			synchronized (lazyResponse) {
//				lazyResponse.wait();
				lazyResponse.wait(timeoutValue*2); // just in case the async request never timeouts
				if (lazyResponse.exception instanceof IOException)
					throw (IOException) lazyResponse.exception;
				if (lazyResponse.exception != null)
					throw new RuntimeException(lazyResponse.exception);
				if (lazyResponse.message == null)
					throw new SocketTimeoutException();
				return lazyResponse.message;
			}
		} catch (InterruptedException e) {
			//FIXME
			error("Interrupted while sending query", e);
			Thread.currentThread().interrupt();
//			throw new InterruptedIOException("asdfadsF");
			return null;
		}
	}

	/**
	 * Asynchronously sends a message to a single server, registering a listener
	 * to receive a callback on success or exception. Multiple asynchronous
	 * lookups can be performed in parallel. Since the callback may be invoked
	 * before the function returns, external synchronization is necessary.
	 * 
	 * @param query
	 *            The query to send
	 * @param listener
	 *            The object containing the callbacks.
	 * @return An identifier, which is also a parameter in the callback
	 */
	public synchronized Object sendAsync(Message query, ResolverListener listener) {
		
		final long deadline = System.currentTimeMillis() + timeoutValue;
		final int id = getMessageId(query);
		if(id == -1) {
			logger.error("SimpleResolver: Couldn't find valid DNS message ID");
			listener.handleException(null, new RuntimeException("Could not find valid DNS message ID"));
		} else {
			contexts.put(id, new ResponseContext(query, listener, deadline));
		}
//		Record question = query.getQuestion();

		query = (Message) query.clone();
		applyEDNS(query);
		if (tsig != null)
			tsig.apply(query, null);

		byte[] out = query.toWire(Message.MAXLENGTH);
		ChannelBuffer buffer = ChannelBuffers.wrappedBuffer(out);
		try {
			ChannelFuture future = channel.write(buffer);
			if (!future.await(timeoutValue))
				handleException(id, new SocketTimeoutException("Write timeout when attempting to send DNS request"));
			else if (!future.isSuccess())
				handleException(id, (Exception)future.getCause());
		} catch (InterruptedException e) {
			handleException(id, e);
			Thread.currentThread().interrupt();
		}
		return id;
	}
	
	private int getMessageId(Message message) {
		int id = message.getHeader().getID();
		int count = 0;
		while(count < 10) {
			if(!contexts.containsKey(id))
				return id;
			count++;
			id = (id + 1) & 0xFFFF;
			message.getHeader().setID(id);
		}
		return -1;
	}

	@SuppressWarnings("unchecked")
	private Message sendAXFR(Message query) throws IOException {
		Name qname = query.getQuestion().getName();
		ZoneTransferIn xfrin = ZoneTransferIn.newAXFR(qname, locator.getAddress().toInetAddress().getHostAddress(), locator.getPort(), tsig);
		xfrin.setTimeout((int) (getTimeout() / 1000));
		try {
			xfrin.run();
		} catch (ZoneTransferException e) {
			throw new WireParseException(e.getMessage());
		}
		List<Record> records = xfrin.getAXFR();
		Message response = new Message(query.getHeader().getID());
		response.getHeader().setFlag(Flags.AA);
		response.getHeader().setFlag(Flags.QR);
		response.addRecord(query.getQuestion(), Section.QUESTION);
		Iterator<Record> it = records.iterator();
		while (it.hasNext())
			response.addRecord(it.next(), Section.ANSWER);
		return response;
	}

	public void setPort(int port) {
		throw new RuntimeException("SimpleResolver cannot implement setPort!");
	}
	
	public synchronized void checkTimeOut() {
		if (contexts.size() == 0)
			return;
		
		long now = System.currentTimeMillis();
		List<Integer> timedOutKeys = new ArrayList<Integer>();
		for (Integer key: contexts.keySet())
			if (now > contexts.get(key).deadline)
				timedOutKeys.add(key);
		for (Integer key: timedOutKeys) {
			ResponseContext context = contexts.remove(key);
			context.listener.handleException(key, new SocketTimeoutException("Request "+key+" timed out"));
		}
	}
	
	public UDPSocketLocator getRemoteAddress() {
		return locator; //new UDPSocketLocator(channel.getRemoteAddress());
	}
	
	public synchronized void shutdown() throws IOException {
		logger.debug("Shutting down resolver, uncomplete requests: "+contexts.size());
		
		if (channel != null)
			channel.close();
		if (timer != null)
			timer.stop();
		for (Integer key: contexts.keySet()) {
			contexts.get(key).listener.handleException(key, new SocketException("The resolver was shut down"));
		}
	}
}
