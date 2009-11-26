package com.netifera.platform.net.http.tools;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;

import org.apache.http.ConnectionReuseStrategy;
import org.apache.http.Header;
import org.apache.http.HttpConnection;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpInetConnection;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseFactory;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.ProtocolVersion;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.nio.DefaultClientIOEventDispatch;
import org.apache.http.impl.nio.DefaultServerIOEventDispatch;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.reactor.DefaultListeningIOReactor;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.NHttpClientConnection;
import org.apache.http.nio.NHttpClientHandler;
import org.apache.http.nio.NHttpConnection;
import org.apache.http.nio.NHttpServerConnection;
import org.apache.http.nio.NHttpServiceHandler;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.nio.reactor.ListeningIOReactor;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.RequestConnControl;
import org.apache.http.protocol.RequestContent;
import org.apache.http.protocol.RequestExpectContinue;
import org.apache.http.protocol.RequestTargetHost;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;

import com.netifera.platform.api.tools.ITool;
import com.netifera.platform.api.tools.IToolContext;
import com.netifera.platform.api.tools.ToolException;
import com.netifera.platform.net.http.internal.tools.Activator;
import com.netifera.platform.tools.RequiredOptionMissingException;
import com.netifera.platform.util.addresses.inet.InternetAddress;
import com.netifera.platform.util.locators.TCPSocketLocator;


public class HTTPProxy implements ITool {
//	public final static String TOOL_NAME = "HTTP Proxy";
//	public final static String TOOL_DESCRIPTION = "Proxy that collects visited URLs and rewrites HTML responses to show hidden fields.";

	private IToolContext context;
	private int listeningPort;

	public void toolRun(IToolContext context) throws ToolException {
		this.context = context;
		
		setupToolOptions();
		try {
			context.info("HTTP proxy listening on port "+listeningPort);
			listen(listeningPort);
		//} catch (InterruptedException e) {
		//	Thread.currentThread().interrupt();
		//	//context.warning(e.getMessage());
		} catch (IOException e) {
			context.exception(e.getMessage(), e);
		}
	}

	private void setupToolOptions() throws ToolException {
		Integer port = (Integer) context.getConfiguration().get("port");
		if (port == null)
			throw new RequiredOptionMissingException("port");
		listeningPort = port.intValue();
	}

	private void interceptResponse(NHttpClientConnection conn, HttpRequest request, HttpResponse response) {
		URI url;
		try {
			url = new URI(request.getRequestLine().getUri());
		} catch (URISyntaxException e) {
			context.debug("bad URI: " + request.getRequestLine().getUri());
			return;
		}
		InetAddress address = ((HttpInetConnection)conn).getRemoteAddress();
		int port = ((HttpInetConnection)conn).getRemotePort();

		TCPSocketLocator locator = new TCPSocketLocator(InternetAddress.fromInetAddress(address), port);
		if (response.containsHeader("Server"))
			Activator.getInstance().getWebEntityFactory().createWebServer(context.getRealm(), context.getSpaceId(), locator, response.getFirstHeader("Server").getValue());
		else
			Activator.getInstance().getWebEntityFactory().createWebServer(context.getRealm(), context.getSpaceId(), locator, null);
		
		HttpEntity entity = response.getEntity();
		int status = response.getStatusLine().getStatusCode();
		if (status < 200 || status >= 400) {
			context.debug("inconsistant: " + request.getRequestLine()+" ->  "+response.getStatusLine().toString());
		} else {
			context.debug(request.getRequestLine()+" ->  "+response.getStatusLine().toString());
			if (status == 200) {
				String contentType = entity.getContentType().getValue();
				Activator.getInstance().getWebEntityFactory().createWebPage(context.getRealm(), context.getSpaceId(), locator, url, contentType);
				
				// is favicon? get it and add it to the model
/*							if (url.getPath().equals("/favicon.ico") && contentType.matches("image/x-icon|application/octet-stream")) {
					entity.consumeContent();
					byte[] favicon = new byte[(int)entity.getContentLength()];
					entity.getContent().read(favicon);
					messenger.WebSiteFaviconDiscovered(service, url, favicon);
				}
*/
//				System.err.println(contentType);
				
			} else if (status >= 300) {
				String location = response.getFirstHeader("Location").getValue();
				context.info("Redirect "+url+" to "+location);
			}
		}
	}

	public void listen(int port) throws IOException {
		HttpParams params = new BasicHttpParams();
		params.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 30000)
				.setIntParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE,
						8 * 1024).setBooleanParameter(
						CoreConnectionPNames.STALE_CONNECTION_CHECK, false)
				.setBooleanParameter(CoreConnectionPNames.TCP_NODELAY, true)
				.setParameter(CoreProtocolPNames.ORIGIN_SERVER,
						"HttpComponents/1.1")/*.setParameter(
						CoreProtocolPNames.USER_AGENT, "HttpComponents/1.1")*/;

		final ConnectingIOReactor connectingIOReactor = new DefaultConnectingIOReactor(
				1, params);

		final ListeningIOReactor listeningIOReactor = new DefaultListeningIOReactor(
				1, params);

		BasicHttpProcessor originServerProc = new BasicHttpProcessor();
		originServerProc.addInterceptor(new RequestContent());
		originServerProc.addInterceptor(new RequestTargetHost());
		originServerProc.addInterceptor(new RequestConnControl());
//		originServerProc.addInterceptor(new RequestUserAgent());
		originServerProc.addInterceptor(new RequestExpectContinue());

		BasicHttpProcessor clientProxyProcessor = new BasicHttpProcessor();
		clientProxyProcessor.addInterceptor(new ResponseDate());
		clientProxyProcessor.addInterceptor(new ResponseServer());
		clientProxyProcessor.addInterceptor(new ResponseContent());
		clientProxyProcessor.addInterceptor(new ResponseConnControl());

		NHttpClientHandler connectingHandler = new ConnectingHandler(
				originServerProc, new DefaultConnectionReuseStrategy(), params);

		NHttpServiceHandler listeningHandler = new ListeningHandler(
				connectingIOReactor, clientProxyProcessor,
				new DefaultHttpResponseFactory(),
				new DefaultConnectionReuseStrategy(), params);

		final IOEventDispatch connectingEventDispatch = new DefaultClientIOEventDispatch(
				connectingHandler, params);

		final IOEventDispatch listeningEventDispatch = new DefaultServerIOEventDispatch(
				listeningHandler, params);

		Thread t = new Thread(new Runnable() {

			public void run() {
				try {
					connectingIOReactor.execute(connectingEventDispatch);
				} catch (InterruptedIOException ex) {
					context.warning("Interrupted");
				} catch (IOException e) {
					context.exception("I/O error: " + e.getMessage(), e);
				}
			}

		});
		t.start();

		listeningIOReactor.listen(new InetSocketAddress(port));
		listeningIOReactor.execute(listeningEventDispatch);
	}
	
	class ListeningHandler implements NHttpServiceHandler {

//		private final HttpHost targetHost;
		private final ConnectingIOReactor connectingIOReactor;
		private final HttpProcessor httpProcessor;
		private final HttpResponseFactory responseFactory;
		private final ConnectionReuseStrategy connStrategy;
		private final HttpParams params;

		public ListeningHandler(
				final ConnectingIOReactor connectingIOReactor,
				final HttpProcessor httpProcessor,
				final HttpResponseFactory responseFactory,
				final ConnectionReuseStrategy connStrategy,
				final HttpParams params) {
			super();
			this.connectingIOReactor = connectingIOReactor;
			this.httpProcessor = httpProcessor;
			this.connStrategy = connStrategy;
			this.responseFactory = responseFactory;
			this.params = params;
		}

		public void connected(final NHttpServerConnection conn) {
			context.debug(conn + ": client conn open");

		}

		public void requestReceived(final NHttpServerConnection conn) {
			context.debug(conn + ": client conn request received");

/*			HttpContext context = conn.getContext();
			ProxyTask proxyTask = (ProxyTask) context
					.getAttribute(ProxyTask.ATTRIB);
*/
			
			ProxyTask proxyTask = new ProxyTask();

			synchronized (proxyTask) {

				// Initialize connection state
//				proxyTask.setTarget(this.targetHost);
				proxyTask.setClientIOControl(conn);
				proxyTask.setClientState(ProxyTask.CONNECTED);

				HttpContext httpContext = conn.getContext();
				httpContext.setAttribute(ProxyTask.ATTRIB, proxyTask);

				/*****************************/
				HttpRequest request = conn.getHttpRequest();
				URI url;
				try {
					url = new URI(request.getRequestLine().getUri());
				} catch (URISyntaxException e) {
					context.debug("bad URI: " + request.getRequestLine().getUri());
					return;
				}
				String host = url.getHost();
				int port = url.getPort();
				if (port < 0) port = 80;
				InetSocketAddress address = new InetSocketAddress(host, port);

/*				TCPServiceLocator service = new TCPServiceLocator(InternetAddress.fromInetAddress(address.getAddress()), port);
				context.setAttribute("service-locator", service);
				context.setAttribute("url", url);
*/				/************************************/
				
				this.connectingIOReactor.connect(address, null, proxyTask, null);
/*			}

			
			synchronized (proxyTask) {
				// Validate connection state
				if (proxyTask.getClientState() != ProxyTask.IDLE
						&& proxyTask.getClientState() != ProxyTask.CONNECTED) {
					throw new IllegalStateException("Illegal connection state");
				}
*/
				try {

//					HttpRequest request = conn.getHttpRequest();

					context.debug(conn + ": [client] >> "
							+ request.getRequestLine().toString());
					Header[] headers = request.getAllHeaders();
					for (int i = 0; i < headers.length; i++) {
						context.debug(conn + ": [client] >> "
								+ headers[i].toString());
					}

					ProtocolVersion ver = request.getRequestLine()
							.getProtocolVersion();
					if (!ver.lessEquals(HttpVersion.HTTP_1_1)) {
						// Downgrade protocol version if greater than HTTP/1.1
						ver = HttpVersion.HTTP_1_1;
					}

					// FIXME should rewrite the request, change http://x/ to /
					// Update connection state
					proxyTask.setRequest(request);
					proxyTask.setClientState(ProxyTask.REQUEST_RECEIVED);

					// See if the client expects a 100-Continue
					if (request instanceof HttpEntityEnclosingRequest) {
						if (((HttpEntityEnclosingRequest) request)
								.expectContinue()) {
							HttpResponse ack = this.responseFactory
									.newHttpResponse(ver,
											HttpStatus.SC_CONTINUE, httpContext);
							conn.submitResponse(ack);
						}
					} else {
						// No request content expected. Suspend client input
						conn.suspendInput();
					}

					// If there is already a connection to the origin server
					// make sure origin output is active
					if (proxyTask.getOriginIOControl() != null) {
						proxyTask.getOriginIOControl().requestOutput();
					}

				} catch (IOException ex) {
					shutdownConnection(conn);
				} catch (HttpException ex) {
					shutdownConnection(conn);
				}
			}
		}

		public void inputReady(final NHttpServerConnection conn,
				final ContentDecoder decoder) {
			context.debug(conn + ": client conn input ready " + decoder);

			HttpContext httpContext = conn.getContext();
			ProxyTask proxyTask = (ProxyTask) httpContext
					.getAttribute(ProxyTask.ATTRIB);

			synchronized (proxyTask) {
				// Validate connection state
				if (proxyTask.getClientState() != ProxyTask.REQUEST_RECEIVED
						&& proxyTask.getClientState() != ProxyTask.REQUEST_BODY_STREAM) {
					throw new IllegalStateException("Illegal connection state");
				}
				try {

					ByteBuffer dst = proxyTask.getInBuffer();
					int bytesRead = decoder.read(dst);
					context.debug(conn + ": " + bytesRead + " bytes read");
					if (!dst.hasRemaining()) {
						// Input buffer is full. Suspend client input
						// until the origin handler frees up some space in the
						// buffer
						conn.suspendInput();
					}
					// If there is some content in the input buffer make sure
					// origin
					// output is active
					if (dst.position() > 0) {
						if (proxyTask.getOriginIOControl() != null) {
							proxyTask.getOriginIOControl().requestOutput();
						}
					}

					if (decoder.isCompleted()) {
						context.debug(conn
								+ ": client conn request body received");
						// Update connection state
						proxyTask.setClientState(ProxyTask.REQUEST_BODY_DONE);
						// Suspend client input
						conn.suspendInput();
					} else {
						proxyTask.setClientState(ProxyTask.REQUEST_BODY_STREAM);
					}

				} catch (IOException ex) {
					shutdownConnection(conn);
				}
			}
		}

		public void responseReady(final NHttpServerConnection conn) {
			context.debug(conn + ": client conn response ready");

			HttpContext httpContext = conn.getContext();
			ProxyTask proxyTask = (ProxyTask) httpContext
					.getAttribute(ProxyTask.ATTRIB);

			synchronized (proxyTask) {
				if (proxyTask.getClientState() == ProxyTask.IDLE) {
					// Response not available
					return;
				}
				// Validate connection state
				if (proxyTask.getClientState() != ProxyTask.REQUEST_RECEIVED
						&& proxyTask.getClientState() != ProxyTask.REQUEST_BODY_DONE) {
					throw new IllegalStateException("Illegal connection state");
				}
				try {

					HttpRequest request = proxyTask.getRequest();
					HttpResponse response = proxyTask.getResponse();
					if (response == null) {
						throw new IllegalStateException("HTTP response is null");
					}
					// Remove connection specific headers
					response.removeHeaders(HTTP.CONTENT_LEN);
					response.removeHeaders(HTTP.TRANSFER_ENCODING);
					response.removeHeaders(HTTP.CONN_DIRECTIVE);
					response.removeHeaders("Keep-Alive");

					response.setParams(this.params);

					// Pre-process HTTP request
					httpContext.setAttribute(ExecutionContext.HTTP_CONNECTION, conn);
					httpContext.setAttribute(ExecutionContext.HTTP_REQUEST, request);
					this.httpProcessor.process(response, httpContext);

					conn.submitResponse(response);

					proxyTask.setClientState(ProxyTask.RESPONSE_SENT);

					context.debug(conn + ": [proxy] << "
							+ response.getStatusLine().toString());
					Header[] headers = response.getAllHeaders();
					for (int i = 0; i < headers.length; i++) {
						context.debug(conn + ": [proxy] << "
								+ headers[i].toString());
					}
//---------- here
					if (!canResponseHaveBody(request, response)) {
						conn.resetInput();
						if (!this.connStrategy.keepAlive(response, httpContext)) {
							conn.close();
						} else {
							// Reset connection state
							proxyTask.reset();
							conn.requestInput();
							// Ready to deal with a new request
						}
					}

				} catch (IOException ex) {
					shutdownConnection(conn);
				} catch (HttpException ex) {
					shutdownConnection(conn);
				}
			}
		}

		private boolean canResponseHaveBody(final HttpRequest request,
				final HttpResponse response) {

			if (request != null
					&& "HEAD".equalsIgnoreCase(request.getRequestLine()
							.getMethod())) {
				return false;
			}

			int status = response.getStatusLine().getStatusCode();
			return status >= HttpStatus.SC_OK
					&& status != HttpStatus.SC_NO_CONTENT
					&& status != HttpStatus.SC_NOT_MODIFIED
					&& status != HttpStatus.SC_RESET_CONTENT;
		}

		public void outputReady(final NHttpServerConnection conn,
				final ContentEncoder encoder) {
			context.debug(conn + ": client conn output ready " + encoder);

			HttpContext httpContext = conn.getContext();
			ProxyTask proxyTask = (ProxyTask) httpContext
					.getAttribute(ProxyTask.ATTRIB);

			synchronized (proxyTask) {
				// Validate connection state
				if (proxyTask.getClientState() != ProxyTask.RESPONSE_SENT
						&& proxyTask.getClientState() != ProxyTask.RESPONSE_BODY_STREAM) {
					throw new IllegalStateException("Illegal connection state");
				}

				HttpResponse response = proxyTask.getResponse();
				if (response == null) {
					throw new IllegalStateException("HTTP request is null");
				}

				try {

					ByteBuffer src = proxyTask.getOutBuffer();
					src.flip();
					if (src.hasRemaining()) {
						int bytesWritten = encoder.write(src);
						context.debug(conn + ": " + bytesWritten
								+ " bytes written");
					}
					src.compact();

					if (src.position() == 0) {
						if (proxyTask.getOriginState() == ProxyTask.RESPONSE_BODY_DONE) {
							encoder.complete();
						} else {
							// Input output is empty. Wait until the origin
							// handler
							// fills up the buffer
							conn.suspendOutput();
						}
					}

					// Update connection state
					if (encoder.isCompleted()) {
						context.debug(conn
								+ ": client conn response body sent");
						proxyTask.setClientState(ProxyTask.RESPONSE_BODY_DONE);
						if (!this.connStrategy.keepAlive(response, httpContext)) {
							conn.close();
						} else {
							// Reset connection state
							proxyTask.reset();
							conn.requestInput();
							// Ready to deal with a new request
						}
					} else {
						proxyTask
								.setOriginState(ProxyTask.RESPONSE_BODY_STREAM);
						// Make sure origin input is active
						proxyTask.getOriginIOControl().requestInput();
					}

				} catch (IOException ex) {
					shutdownConnection(conn);
				}
			}
		}

		public void closed(final NHttpServerConnection conn) {
			context.debug(conn + ": client conn closed");
			HttpContext httpContext = conn.getContext();
			ProxyTask proxyTask = (ProxyTask) httpContext
					.getAttribute(ProxyTask.ATTRIB);

			if (proxyTask != null) {
				synchronized (proxyTask) {
					IOControl ioControl = proxyTask.getOriginIOControl();
					if (ioControl != null) {
						try {
							ioControl.shutdown();
						} catch (IOException ex) {
							// ignore
						}
					}
				}
			}
		}

		public void exception(final NHttpServerConnection conn,
				final HttpException httpex) {
			context.debug(conn + ": " + httpex.getMessage());

			HttpContext httpContext = conn.getContext();

			try {
				HttpResponse response = this.responseFactory.newHttpResponse(
						HttpVersion.HTTP_1_0, HttpStatus.SC_BAD_REQUEST,
						httpContext);
				response.setParams(this.params);
				response.addHeader(HTTP.CONN_DIRECTIVE, HTTP.CONN_CLOSE);
				// Pre-process HTTP request
				httpContext.setAttribute(ExecutionContext.HTTP_CONNECTION, conn);
				httpContext.setAttribute(ExecutionContext.HTTP_REQUEST, null);
				this.httpProcessor.process(response, httpContext);

				conn.submitResponse(response);

				conn.close();

			} catch (IOException ex) {
				shutdownConnection(conn);
			} catch (HttpException ex) {
				shutdownConnection(conn);
			}
		}

		public void exception(final NHttpServerConnection conn,
				final IOException ex) {
			shutdownConnection(conn);
			context.debug(conn + ": " + ex.getMessage());
		}

		public void timeout(final NHttpServerConnection conn) {
			context.debug(conn + ": timeout"); // FIXME info?
			shutdownConnection(conn);
		}

		private void shutdownConnection(final NHttpConnection conn) {
			try {
				conn.shutdown();
			} catch (IOException ignore) {
			}
		}
	}

	class ConnectingHandler implements NHttpClientHandler {

		private final HttpProcessor httpProcessor;
		private final ConnectionReuseStrategy connStrategy;
		private final HttpParams params;

		public ConnectingHandler(final HttpProcessor httpProcessor,
				final ConnectionReuseStrategy connStrategy,
				final HttpParams params) {
			super();
			this.httpProcessor = httpProcessor;
			this.connStrategy = connStrategy;
			this.params = params;
		}

		public void connected(final NHttpClientConnection conn,
				final Object attachment) {
			context.debug(conn + ": origin conn open");

			// The shared state object is expected to be passed as an attachment
			ProxyTask proxyTask = (ProxyTask) attachment;

			synchronized (proxyTask) {
				// Validate connection state
				if (proxyTask.getOriginState() != ProxyTask.IDLE) {
					throw new IllegalStateException("Illegal connection state");
				}
				// Set origin IO control handle
				proxyTask.setOriginIOControl(conn);
				// Store the state object in the context
				HttpContext context = conn.getContext();
				context.setAttribute(ProxyTask.ATTRIB, proxyTask);
				// Update connection state
				proxyTask.setOriginState(ProxyTask.CONNECTED);
				
				if (proxyTask.getRequest() != null) {
					conn.requestOutput();
				}
			}
		}

		public void requestReady(final NHttpClientConnection conn) {
			context.debug(conn + ": origin conn request ready");

			HttpContext httpContext = conn.getContext();
			ProxyTask proxyTask = (ProxyTask) httpContext
					.getAttribute(ProxyTask.ATTRIB);

			synchronized (proxyTask) {
				// Validate connection state
				if (proxyTask.getOriginState() == ProxyTask.REQUEST_SENT) {
					// Request sent but no response available yet
					return;
				}
				if (proxyTask.getOriginState() != ProxyTask.IDLE
						&& proxyTask.getOriginState() != ProxyTask.CONNECTED) {
					throw new IllegalStateException("Illegal connection state");
				}

				HttpRequest request = proxyTask.getRequest();
				if (request == null) {
					throw new IllegalStateException("HTTP request is null");
				}

				// Remove connection specific headers
				request.removeHeaders(HTTP.CONTENT_LEN);
				request.removeHeaders(HTTP.TRANSFER_ENCODING);
				request.removeHeaders(HTTP.TARGET_HOST);
				request.removeHeaders(HTTP.CONN_DIRECTIVE);
				request.removeHeaders(HTTP.USER_AGENT);
				request.removeHeaders("Keep-Alive");

				HttpHost targetHost = proxyTask.getTarget();

				try {
					// FIXME
					// should change the URL from http://google.com/ to /
					request.setParams(this.params);

					// Pre-process HTTP request
					httpContext.setAttribute(ExecutionContext.HTTP_CONNECTION,conn);
					httpContext.setAttribute(ExecutionContext.HTTP_TARGET_HOST,targetHost);

					this.httpProcessor.process(request, httpContext);
					// and send it to the origin server
					conn.submitRequest(request);
					// Update connection state
					proxyTask.setOriginState(ProxyTask.REQUEST_SENT);

					context.debug(conn + ": [proxy] >> "
							+ request.getRequestLine().toString());
					Header[] headers = request.getAllHeaders();
					for (int i = 0; i < headers.length; i++) {
						context.debug(conn + ": [proxy] >> "
								+ headers[i].toString());
					}

				} catch (IOException ex) {
					shutdownConnection(conn);
				} catch (HttpException ex) {
					shutdownConnection(conn);
				}
			}
		}

		public void outputReady(final NHttpClientConnection conn,
				final ContentEncoder encoder) {
			context.debug(conn + ": origin conn output ready " + encoder);

			HttpContext httpContext = conn.getContext();
			ProxyTask proxyTask = (ProxyTask) httpContext
					.getAttribute(ProxyTask.ATTRIB);

			synchronized (proxyTask) {
				// Validate connection state
				if (proxyTask.getOriginState() != ProxyTask.REQUEST_SENT
						&& proxyTask.getOriginState() != ProxyTask.REQUEST_BODY_STREAM) {
					throw new IllegalStateException("Illegal connection state");
				}
				try {

					ByteBuffer src = proxyTask.getInBuffer();
					src.flip();
					if (src.hasRemaining()) {
						int bytesWritten = encoder.write(src);
						context.debug(conn + ": " + bytesWritten
								+ " bytes written");
					}
					src.compact();

					if (src.position() == 0) {
						if (proxyTask.getClientState() == ProxyTask.REQUEST_BODY_DONE) {
							encoder.complete();
						} else {
							// Input buffer is empty. Wait until the client
							// fills up
							// the buffer
							conn.suspendOutput();
						}
					}
					// Update connection state
					if (encoder.isCompleted()) {
						context.debug(conn
								+ ": origin conn request body sent");
						proxyTask.setOriginState(ProxyTask.REQUEST_BODY_DONE);
					} else {
						proxyTask.setOriginState(ProxyTask.REQUEST_BODY_STREAM);
						// Make sure client input is active
						proxyTask.getClientIOControl().requestInput();
					}

				} catch (IOException ex) {
					shutdownConnection(conn);
				}
			}
		}

		public void responseReceived(final NHttpClientConnection conn) {
			context.debug(conn + ": origin conn response received");

			HttpContext httpContext = conn.getContext();
			ProxyTask proxyTask = (ProxyTask) httpContext
					.getAttribute(ProxyTask.ATTRIB);

			synchronized (proxyTask) {
				// Validate connection state
				if (proxyTask.getOriginState() != ProxyTask.REQUEST_SENT
						&& proxyTask.getOriginState() != ProxyTask.REQUEST_BODY_DONE) {
					throw new IllegalStateException("Illegal connection state");
				}

				HttpResponse response = conn.getHttpResponse();
				HttpRequest request = proxyTask.getRequest();

				context.debug(conn + ": [origin] << "
						+ response.getStatusLine().toString());
				Header[] headers = response.getAllHeaders();
				for (int i = 0; i < headers.length; i++) {
					context.debug(conn + ": [origin] << "
							+ headers[i].toString());
				}

				int statusCode = response.getStatusLine().getStatusCode();
				if (statusCode < HttpStatus.SC_OK) {
					// Ignore 1xx response
					return;
				}
				try {

					// Update connection state
					proxyTask.setResponse(response);
					proxyTask.setOriginState(ProxyTask.RESPONSE_RECEIVED);

					/**************** zardoz ****************/
					interceptResponse(conn, request, response);
					/**************************************/
					
					if (!canResponseHaveBody(request, response)) {
						conn.resetInput();
						if (!this.connStrategy.keepAlive(response, httpContext)) {
							conn.close();
						}
					}
					// Make sure client output is active
					proxyTask.getClientIOControl().requestOutput();

				} catch (IOException ex) {
					shutdownConnection(conn);
				}
			}

		}

		private boolean canResponseHaveBody(final HttpRequest request,
				final HttpResponse response) {

			if (request != null
					&& "HEAD".equalsIgnoreCase(request.getRequestLine()
							.getMethod())) {
				return false;
			}

			int status = response.getStatusLine().getStatusCode();
			return status >= HttpStatus.SC_OK
					&& status != HttpStatus.SC_NO_CONTENT
					&& status != HttpStatus.SC_NOT_MODIFIED
					&& status != HttpStatus.SC_RESET_CONTENT;
		}

		public void inputReady(final NHttpClientConnection conn,
				final ContentDecoder decoder) {
			context.debug(conn + ": origin conn input ready " + decoder);

			HttpContext httpContext = conn.getContext();
			ProxyTask proxyTask = (ProxyTask) httpContext
					.getAttribute(ProxyTask.ATTRIB);

			synchronized (proxyTask) {
				// Validate connection state
				if (proxyTask.getOriginState() != ProxyTask.RESPONSE_RECEIVED
						&& proxyTask.getOriginState() != ProxyTask.RESPONSE_BODY_STREAM) {
					throw new IllegalStateException("Illegal connection state");
				}
				HttpResponse response = proxyTask.getResponse();
				try {

					ByteBuffer dst = proxyTask.getOutBuffer();
					int bytesRead = decoder.read(dst);
					context.debug(conn + ": " + bytesRead + " bytes read");
					if (!dst.hasRemaining()) {
						// Output buffer is full. Suspend origin input until
						// the client handler frees up some space in the buffer
						conn.suspendInput();
					}
					// If there is some content in the buffer make sure client
					// output
					// is active
					if (dst.position() > 0) {
						proxyTask.getClientIOControl().requestOutput();
					}

					if (decoder.isCompleted()) {
						context.debug(conn
								+ ": origin conn response body received");
						proxyTask.setOriginState(ProxyTask.RESPONSE_BODY_DONE);
						if (!this.connStrategy.keepAlive(response, httpContext)) {
							conn.close();
						}
					} else {
						proxyTask
								.setOriginState(ProxyTask.RESPONSE_BODY_STREAM);
					}

				} catch (IOException ex) {
					shutdownConnection(conn);
				}
			}
		}

		public void closed(final NHttpClientConnection conn) {
			context.debug(conn + ": origin conn closed");
			HttpContext httpContext = conn.getContext();
			ProxyTask proxyTask = (ProxyTask) httpContext
					.getAttribute(ProxyTask.ATTRIB);

			if (proxyTask != null) {
				synchronized (proxyTask) {
					IOControl ioControl = proxyTask.getClientIOControl();
					if (ioControl != null) {
						try {
							ioControl.shutdown();
						} catch (IOException ignore) {
						}
					}
				}
			}
		}

		public void exception(final NHttpClientConnection conn,
				final HttpException ex) {
			shutdownConnection(conn);
			context.debug(conn + ": " + ex.getMessage());
		}

		public void exception(final NHttpClientConnection conn,
				final IOException ex) {
			shutdownConnection(conn);
			context.debug(conn + ": " + ex.getMessage());
		}

		public void timeout(final NHttpClientConnection conn) {
			context.debug(conn + ": timeout");
			shutdownConnection(conn);
		}

		private void shutdownConnection(final HttpConnection conn) {
			try {
				conn.shutdown();
			} catch (IOException ignore) {
			}
		}

	}

	class ProxyTask {

		public static final String ATTRIB = "nhttp.proxy-task";

		public static final int IDLE = 0;
		public static final int CONNECTED = 1;
		public static final int REQUEST_RECEIVED = 2;
		public static final int REQUEST_SENT = 3;
		public static final int REQUEST_BODY_STREAM = 4;
		public static final int REQUEST_BODY_DONE = 5;
		public static final int RESPONSE_RECEIVED = 6;
		public static final int RESPONSE_SENT = 7;
		public static final int RESPONSE_BODY_STREAM = 8;
		public static final int RESPONSE_BODY_DONE = 9;

		private final ByteBuffer inBuffer;
		private final ByteBuffer outBuffer;

		private HttpHost target;

		private IOControl originIOControl;
		private IOControl clientIOControl;

		private int originState;
		private int clientState;

		private HttpRequest request;
		private HttpResponse response;

		public ProxyTask() {
			super();
			this.originState = IDLE;
			this.clientState = IDLE;
			this.inBuffer = ByteBuffer.allocateDirect(10240);
			this.outBuffer = ByteBuffer.allocateDirect(10240);
		}

		public ByteBuffer getInBuffer() {
			return this.inBuffer;
		}

		public ByteBuffer getOutBuffer() {
			return this.outBuffer;
		}

		public HttpHost getTarget() {
			return this.target;
		}

		public void setTarget(final HttpHost target) {
			this.target = target;
		}

		public HttpRequest getRequest() {
			return this.request;
		}

		public void setRequest(final HttpRequest request) {
			this.request = request;
		}

		public HttpResponse getResponse() {
			return this.response;
		}

		public void setResponse(final HttpResponse response) {
			this.response = response;
		}

		public IOControl getClientIOControl() {
			return this.clientIOControl;
		}

		public void setClientIOControl(final IOControl clientIOControl) {
			this.clientIOControl = clientIOControl;
		}

		public IOControl getOriginIOControl() {
			return this.originIOControl;
		}

		public void setOriginIOControl(final IOControl originIOControl) {
			this.originIOControl = originIOControl;
		}

		public int getOriginState() {
			return this.originState;
		}

		public void setOriginState(int state) {
			this.originState = state;
		}

		public int getClientState() {
			return this.clientState;
		}

		public void setClientState(int state) {
			this.clientState = state;
		}

		public void reset() {
			this.inBuffer.clear();
			this.outBuffer.clear();
			this.originState = IDLE;
			this.clientState = IDLE;
			this.request = null;
			this.response = null;
		}

		public void shutdown() {
			if (this.clientIOControl != null) {
				try {
					this.clientIOControl.shutdown();
				} catch (IOException ignore) {
				}
			}
			if (this.originIOControl != null) {
				try {
					this.originIOControl.shutdown();
				} catch (IOException ignore) {
				}
			}
		}
	}
}
