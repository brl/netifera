package com.netifera.platform.ui.internal.interop;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpServerConnection;
import org.apache.http.HttpStatus;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.entity.ContentProducer;
import org.apache.http.entity.EntityTemplate;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.DefaultHttpServerConnection;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.HttpRequestHandlerRegistry;
import org.apache.http.protocol.HttpService;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;
import org.apache.http.util.EntityUtils;
import org.osgi.service.component.ComponentContext;

import com.netifera.platform.api.log.ILogManager;
import com.netifera.platform.api.log.ILogger;
import com.netifera.platform.net.model.InternetAddressEntity;
import com.netifera.platform.probebuild.api.IProbeConfiguration;
import com.netifera.platform.probebuild.api.IProbeDeployable;
import com.netifera.platform.util.addresses.inet.InternetAddress;

public class InterOpService {

	private ILogger logger;
	
	private Thread listenerThread;

	protected void activate(ComponentContext context) {
		try {
			listenerThread = new RequestListenerThread(0x371f);
			listenerThread.setDaemon(false);
			listenerThread.start();
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	protected void deactivate(ComponentContext context) {
		listenerThread.interrupt();
	}

	protected void setLogManager(ILogManager logManager) {
		this.logger = logManager.getLogger("HTTP Interoperability Service");
		logger.enableDebug();
	}
	
	protected void unsetLogManager(ILogManager logManager) {
	}

	class GetProbeHandler implements HttpRequestHandler  {
		
		public void handle(
				final HttpRequest request, 
				final HttpResponse response,
				final HttpContext context) throws HttpException, IOException {

			String method = request.getRequestLine().getMethod().toUpperCase(Locale.ENGLISH);
			if (!method.equals("GET")) {
				throw new MethodNotSupportedException(method + " method not supported"); 
			}

			URI uri;
			try {
				uri = new URI(request.getRequestLine().getUri());
			} catch (URISyntaxException e1) {
				logger.error("Client requested malformed URI",e1);
				handleNotFound("",response);
				return;
			}

/*			if (request instanceof HttpEntityEnclosingRequest) {
				HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
				byte[] entityContent = EntityUtils.toByteArray(entity);
				logger.debug("Incoming entity content (bytes): " + entityContent.length);
			}
*/
			String path = uri.getPath();
			if (path.startsWith("/"))
				path = path.substring(1);
			
			if (path.length() == 0) {
				handleNotFound(path, response);
				return;
			}

			String[] parts = path.split("/");

			if (parts.length == 1) {
				// list available probe configurations
				response.setStatusCode(HttpStatus.SC_OK);
					EntityTemplate body = new EntityTemplate(new ContentProducer() {
						public void writeTo(final OutputStream outstream) throws IOException {
							OutputStreamWriter writer = new OutputStreamWriter(outstream, "UTF-8"); 
							for (String name: Activator.getInstance().getProbeBuilder().listProbeConfigurations()) {
								writer.write(name);
								writer.write("\n");
							}
							writer.flush();
						}
					});
				body.setContentType("text/html; charset=UTF-8");
				response.setEntity(body);
				return;
			}
			
			if (parts.length < 4) {
				handleNotFound(path, response);
				return;
			}

			IProbeConfiguration probeConfig;
			try {
				probeConfig = Activator.getInstance().getProbeBuilder().getProbeConfiguration(parts[1]);
			} catch (IOException e) {
				logger.error("Error while retrieving probe configuration '"+parts[1]+"'", e);
				handleNotFound(path, response);
				return;
			}

			if (!isSupportedPlatform(parts[2]) || !isSupportedFormat(parts[3])) {
				handleNotFound(path, response);
				return;
			}

			logger.info("Generating probe");
			
			IProbeDeployable deployable = Activator.getInstance().getProbeBuilder().getProbeDeployable(probeConfig, "linux/i386", "ELF32");
			
			byte[] buffer = new byte[1024*1024];
			try {
				InputStream inputStream = deployable.getInputStream();
				File file = File.createTempFile("probe", "interop");
				try {
//					context.setTotalWork(...);
					FileOutputStream outputStream = new FileOutputStream(file);
					int count = 0;
					int result;
					while ((result = inputStream.read(buffer)) > 0) {
						count += result;
						outputStream.write(buffer, 0, result);
					}
					outputStream.close();
					logger.info("Probe successfuly generated, "+count+" bytes");

					response.setStatusCode(HttpStatus.SC_OK);
					FileEntity body = new FileEntity(file, "text/html");
					response.setEntity(body);
					logger.debug("Serving file " + file.getPath());
				} finally {
//					file.delete();//XXX delete it before it finishes? fuck, where to delete it?
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				logger.error("Error while serving probe", e);
			}
		}

		boolean isSupportedPlatform(String platform) {
			return platform.equals("linux-i386");
		}
		
		boolean isSupportedFormat(String format) {
			return format.equals("executable");
		}
	}

	class CreateProbeHandler implements HttpRequestHandler  {
		
		public void handle(
				final HttpRequest request, 
				final HttpResponse response,
				final HttpContext context) throws HttpException, IOException {

			String method = request.getRequestLine().getMethod().toUpperCase(Locale.ENGLISH);
			if (!method.equals("GET")) {
				throw new MethodNotSupportedException(method + " method not supported"); 
			}

			URI uri;
			try {
				uri = new URI(request.getRequestLine().getUri());
			} catch (URISyntaxException e1) {
				logger.error("Client requested malformed URI",e1);
				handleNotFound("",response);
				return;
			}

			if (request instanceof HttpEntityEnclosingRequest) {
				HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
				byte[] entityContent = EntityUtils.toByteArray(entity);
				logger.debug("Incoming entity content (bytes): " + entityContent.length);
			}

			String path = uri.getPath();
			if (path.startsWith("/"))
				path = path.substring(1);
			
			if (path.length() == 0) {
				handleNotFound(path, response);
				return;
			}

			Map<String,String> query = getQueryParameters(uri.getQuery());

			InternetAddress address = InternetAddress.fromString(query.get("host"));
			String channelConfig = query.get("channel");
			String probeName = query.get("name");

			//XXX hardcoded first space, local probe
			long spaceId = 1;
			long realm = 1;
			
			InternetAddressEntity addressEntity = Activator.getInstance().getNetworkEntityFactory().createAddress(realm, spaceId, address);
			Activator.getInstance().getProbeManager().createProbe(addressEntity.getHost(), probeName != null ? probeName : "Remote Probe", channelConfig, spaceId);
		}
	}
	
	Map<String,String> getQueryParameters(String query) {
		Map<String,String> answer = new HashMap<String,String>();
		for (String parameter: query.split("&")) {
			String[] pair = parameter.split("=");
			answer.put(pair[0], URLDecoder.decode(pair[1]));
		}
		return answer;
	}

	void handleNotFound(final String path, HttpResponse response) {
		response.setStatusCode(HttpStatus.SC_NOT_FOUND);
		EntityTemplate body = new EntityTemplate(new ContentProducer() {
			public void writeTo(final OutputStream outstream) throws IOException {
				OutputStreamWriter writer = new OutputStreamWriter(outstream, "UTF-8"); 
				writer.write("<html><body><h1>");
				writer.write(path);
				writer.write(" not found");
				writer.write("</h1></body></html>");
				writer.flush();
			}
		});
		body.setContentType("text/html; charset=UTF-8");
		response.setEntity(body);
	}

	class RequestListenerThread extends Thread {

		private final ServerSocket socket;
		private final HttpParams params; 
		private final HttpService service;
		
		public RequestListenerThread(int port) throws IOException {
			super();
			this.socket = new ServerSocket(port, 10, InetAddress.getLocalHost());
			this.params = new BasicHttpParams();
			this.params
				.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 5000)
				.setIntParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE, 8 * 1024)
				.setBooleanParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK, false)
				.setBooleanParameter(CoreConnectionPNames.TCP_NODELAY, true)
				.setParameter(CoreProtocolPNames.ORIGIN_SERVER, "HttpComponents/1.1");

			// Set up the HTTP protocol processor
			BasicHttpProcessor httpproc = new BasicHttpProcessor();
			httpproc.addInterceptor(new ResponseDate());
			httpproc.addInterceptor(new ResponseServer());
			httpproc.addInterceptor(new ResponseContent());
			httpproc.addInterceptor(new ResponseConnControl());
			
			// Set up request handlers
			HttpRequestHandlerRegistry reqistry = new HttpRequestHandlerRegistry();
			reqistry.register("/probes/*", new GetProbeHandler());
			reqistry.register("/create_probe", new CreateProbeHandler());
			
			// Set up the HTTP service
			this.service = new HttpService(
					httpproc, 
					new DefaultConnectionReuseStrategy(), 
					new DefaultHttpResponseFactory());
			this.service.setParams(this.params);
			this.service.setHandlerResolver(reqistry);
		}
		
		public void run() {
			logger.info("Listening on port " + this.socket.getLocalPort());
			while (!Thread.interrupted()) {
				try {
					// Set up HTTP connection
					Socket socket = this.socket.accept();
					DefaultHttpServerConnection conn = new DefaultHttpServerConnection();
					logger.info("Incoming connection from " + socket.getInetAddress());
					conn.bind(socket, this.params);

					// Start worker thread
					Thread t = new WorkerThread(this.service, conn);
					t.setDaemon(true);
					t.start();
				} catch (InterruptedIOException ex) {
					break;
				} catch (IOException e) {
					logger.error("I/O error initialising connection thread: " 
							+ e.getMessage(), e);
					break;
				}
			}
		}
	}
	
	class WorkerThread extends Thread {

		private final HttpService service;
		private final HttpServerConnection connection;
		
		public WorkerThread(
				final HttpService service, 
				final HttpServerConnection connection) {
			super();
			this.service = service;
			this.connection = connection;
		}
		
		public void run() {
			logger.debug("New connection thread");
			HttpContext context = new BasicHttpContext(null);
			try {
				while (!Thread.interrupted() && this.connection.isOpen()) {
					this.service.handleRequest(this.connection, context);
				}
			} catch (ConnectionClosedException ex) {
				logger.debug("Client closed connection");
			} catch (IOException ex) {
				logger.error("I/O error: " + ex.getMessage(), ex);
			} catch (HttpException ex) {
				logger.error("Unrecoverable HTTP protocol violation: " + ex.getMessage(), ex);
			} finally {
				try {
					this.connection.shutdown();
				} catch (IOException ignore) {}
			}
		}
	}
}
