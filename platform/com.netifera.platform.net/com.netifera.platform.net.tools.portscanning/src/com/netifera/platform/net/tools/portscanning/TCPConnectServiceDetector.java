package com.netifera.platform.net.tools.portscanning;

import java.io.IOException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.netifera.platform.api.log.ILogger;
import com.netifera.platform.net.internal.tools.portscanning.Activator;
import com.netifera.platform.net.sockets.TCPChannel;
import com.netifera.platform.util.asynchronous.CompletionHandler;
import com.netifera.platform.util.locators.TCPSocketLocator;

public class TCPConnectServiceDetector {

	public final static int CONNECT_TIMEOUT = 5000;
	public final static int READ_BANNER_TIMEOUT = 100;
	public final static int WRITE_TRIGGER_TIMEOUT = 5000;
	public final static int READ_RESPONSE_TIMEOUT = 10000;
	public final static int SHORT_READ_TIMEOUT = READ_BANNER_TIMEOUT;
	
	private final TCPSocketLocator locator;
	
	private TCPChannel channel;
	private final ByteBuffer readBuffer = ByteBuffer.allocate(1024 * 8);
	private final ByteBuffer writeBuffer = ByteBuffer.allocate(1024 * 4);
	private Map<String,String> serviceInfo = null;
	private volatile Future<?> future;

	private final ILogger logger;
	
	private ITCPConnectServiceDetectorListener listener;

	
	TCPConnectServiceDetector(TCPSocketLocator locator, ILogger logger) {
		this.locator = locator;
		this.logger = logger;
	}
	
	public void detect(ITCPConnectServiceDetectorListener listener) throws IOException, InterruptedException {
		this.listener = listener;
		connect();
	}

	public synchronized void cancel(boolean mayInterruptIfRunning) {
		Future<?> future = this.future;
		if (future != null)
			future.cancel(mayInterruptIfRunning);
		logger.debug("Cancelled connection to "+locator);
	}

	/***************************************************************************?*/
	
	private synchronized void connect() throws IOException, InterruptedException {
		try {
			channel = Activator.getInstance().getSocketEngine().openTCP();
			listener.connecting(locator);
			future = channel.connect(locator, CONNECT_TIMEOUT, TimeUnit.MILLISECONDS, null, new CompletionHandler<Void,Void>() {
				public void cancelled(Void attachment) {
					done();
				}
				public void completed(Void result, Void attachment) {
					logger.debug("Connected to "+locator);
					listener.connected(locator);
					readBanner();
				}
				public void failed(final Throwable e, Void attachment) {
					if ((e instanceof ConnectException)
							|| (e instanceof SocketTimeoutException)) {
						/*
						 * ConnectException = closed or rejected
						 * SocketTimeoutException = filtered or no such host
						 */
					} else if (e instanceof NoRouteToHostException || e instanceof SocketException) {
						listener.badTarget(locator);
					} else {
						logger.error("Unexpected exception "+e);
					}
					done();
				}
			});
		} catch (IOException e) {
			done();
			throw e;
		} catch (InterruptedException e) {
			done();
			throw e;
		}
	}
	
	private synchronized void readBanner() {
		// if we wont send a trigger, wait longer for a banner
		byte[] trigger = Activator.getInstance().getServerDetector().getTrigger("tcp",locator.getPort());
		int timeout = trigger.length > 0 ? READ_BANNER_TIMEOUT : READ_RESPONSE_TIMEOUT;
		future = channel.read(readBuffer, timeout, TimeUnit.MILLISECONDS, null, new CompletionHandler<Integer,Void>() {
			public void cancelled(Void attachment) {
				checkUnrecognized();
				done();
			}
			public void completed(Integer result, Void attachment) {
				if (result > 0) {
					ByteBuffer tempBuffer = readBuffer.duplicate();
					tempBuffer.flip();
					serviceInfo = Activator.getInstance().getServerDetector().detect("tcp", locator.getPort(), null, tempBuffer);
					if (serviceInfo != null) {
						listener.serviceDetected(locator, serviceInfo);
						done();
						return;
					}
				} else {
					if (result == -1) {
//						logger.debug(locator + " disconnected before trigger");
						done();
						return;
					}

//					logger.debug(locator + " zero read (no banner)");
				}

				writeTrigger();
			}
			public void failed(Throwable e, Void attachment) {
				if (e instanceof SocketTimeoutException) {
					writeTrigger();
				} else {
					if (! (e instanceof ClosedChannelException)) {
						logger.error("Unexpected error reading banner " + locator, e);
					}
					done();
				}
			}
		});
	}
	
	private synchronized void writeTrigger() {
		byte[] trigger = Activator.getInstance().getServerDetector().getTrigger("tcp",locator.getPort());
		writeBuffer.clear();
		writeBuffer.put(trigger);
		writeBuffer.flip();

		future = channel.write(writeBuffer, WRITE_TRIGGER_TIMEOUT, TimeUnit.MILLISECONDS, null, new CompletionHandler<Integer,Void>() {
			public void cancelled(Void attachment) {
				done();
			}
			public void completed(Integer result, Void attachment) {
				readResponse();
			}
			public void failed(Throwable e, Void attachment) {
				if (! (e instanceof ClosedChannelException)) {
					logger.error("Unexpected error writting trigger " + locator, e);
				}
				checkUnrecognized();
				done();
			}
		});
	}
	
	private synchronized void readResponse() {
		future = channel.read(readBuffer, READ_RESPONSE_TIMEOUT, TimeUnit.MILLISECONDS, null, new CompletionHandler<Integer,Void>() {
			public void cancelled(Void attachment) {
				checkUnrecognized();
				done();
			}
			public void completed(final Integer result, Void attachment) {
				if (result > 0) {
					// read once more, timeout very short
					// sometimes there's some more data right after, comming in another packet
					future = channel.read(readBuffer, SHORT_READ_TIMEOUT, TimeUnit.MILLISECONDS, null, new CompletionHandler<Integer,Void>() {
						public void cancelled(Void attachment) {
							detect();
						}
						public void completed(Integer result,
								Void attachment) {
							detect();
						}
						public void failed(Throwable exc, Void attachment) {
							detect();
						}
						private void detect() {
							readBuffer.flip();
							writeBuffer.rewind();
							serviceInfo = Activator.getInstance().getServerDetector().detect("tcp", locator.getPort(), writeBuffer, readBuffer);
							if (serviceInfo != null) {
								listener.serviceDetected(locator, serviceInfo);
							}
							checkUnrecognized();
							done();
						}
					});
				} else {
					if (result == -1) {
//						logger.debug(locator + " disconnected");
					} else {
//						logger.debug(locator + " 0 read after trigger");
					}
					checkUnrecognized();
					done();
				}
			}
			public void failed(Throwable e, Void attachment) {
				if (e instanceof SocketTimeoutException) {
//					logger.debug(locator + " trigger timeout");
				} else if (! (e instanceof ClosedChannelException)) {
					logger.error("Unexpected error when reading response: " + locator, e);
				}
				checkUnrecognized();
				done();
			}
		});
	}
	
	private synchronized void done() {
		if (channel != null)
			try {
				channel.close();
			} catch (IOException e) {
			}

		listener.finished(locator);
		future = null;
		
		if (serviceInfo != null)
			logger.info(serviceInfo.get("serviceType")+" @ "+locator);
	}
	
	private void checkUnrecognized() {
		// if the service was not recognized
		if (serviceInfo == null)
			logger.warning("Unknown service @ " + locator);
	}
}
