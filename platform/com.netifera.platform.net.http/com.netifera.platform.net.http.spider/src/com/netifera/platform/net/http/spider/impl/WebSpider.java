package com.netifera.platform.net.http.spider.impl;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.nio.protocol.HttpRequestExecutionHandler;
import org.apache.http.nio.reactor.SessionRequest;
import org.apache.http.nio.reactor.SessionRequestCallback;
import org.apache.http.protocol.HttpContext;

import com.netifera.platform.api.log.ILogger;
import com.netifera.platform.net.dns.service.nameresolver.INameResolver;
import com.netifera.platform.net.http.service.AsynchronousHTTPClient;
import com.netifera.platform.net.http.service.HTTP;
import com.netifera.platform.net.http.spider.HTTPRequest;
import com.netifera.platform.net.http.spider.HTTPResponse;
import com.netifera.platform.net.http.spider.IWebSpider;
import com.netifera.platform.net.http.spider.IWebSpiderContext;
import com.netifera.platform.net.http.spider.IWebSpiderModule;
import com.netifera.platform.net.http.spider.OutOfScopeException;
import com.netifera.platform.net.http.web.model.IWebEntityFactory;
import com.netifera.platform.net.http.web.model.WebPageEntity;
import com.netifera.platform.net.http.web.model.WebSiteEntity;
import com.netifera.platform.util.addresses.inet.InternetAddress;
import com.netifera.platform.util.locators.TCPSocketLocator;
import com.netifera.platform.util.patternmatching.InternetAddressMatcher;

public class WebSpider implements IWebSpider {
	private volatile boolean followLinks = true;
	private volatile boolean fetchImages = false;
	private volatile int maximumConnections = 5;
	private volatile int bufferSize = 1024*16;
	private volatile int queueSize = 100;
	
	private long realm;
	private long spaceId;

	private final Map<WebSite, WebSpiderWorker> workers = new HashMap<WebSite, WebSpiderWorker>();

	private volatile int successCount = 0;
	private volatile int errorsCount = 0;
	private volatile boolean interrupted = false;
	
	private List<IWebSpiderModule> modules = new ArrayList<IWebSpiderModule>();

	private ILogger logger;
	private IWebEntityFactory factory;
	private INameResolver resolver;

	
	class WebSpiderWorker implements HttpRequestExecutionHandler {

		private final HTTP http;
		private String vhost;
		private URI base;
		
		private final Queue<URI> queue = new LinkedList<URI>();
		private final BloomFilter knownPaths = new BloomFilter(1024*1024); // 1M per target in scope
		
		private AsynchronousHTTPClient client;

		public WebSpiderWorker(HTTP http, String vhost) {
			this.http = http;
			this.vhost = vhost;
			
			int port = http.getLocator().getPort();
			this.base = URI.create(http.getURIScheme()+"://"+vhost+(port == 80 ? "" : ":"+port)+"/");
		}
		
		IWebSpiderContext getContext() {
			return new IWebSpiderContext() {
				public TCPSocketLocator getSocketLocator() {
					return http.getLocator();
				}

				public URI getBaseURL() {
					return base;
				}

				public long getRealm() {
					return realm;
				}
				
				public long getSpaceId() {
					return spaceId;
				}

				public IWebSpider getSpider() {
					return WebSpider.this;
				}
				
				public ILogger getLogger() {
					return logger;
				}
			};
		}
		
		public synchronized void addURL(URI url) {
			if (queue.size() > queueSize) {
//				toolContext.debug("Queue overflow, ignoring "+url);
				return;
			}
			
			String path = url.getPath();
			if (path.length() == 0)
				path = "/";
			//FIXME what if we want to send again the same request with different query parameters?
			if (knownPaths.add(path)) // if returns true, it means it was already added to the queue before, already crawled or enqueued
				return;
			queue.add(url);
		}
		
		private synchronized void retryURL(URI url) {
			logger.warning("Retrying "+url);
			if (queue.size() > queueSize) {
//				toolContext.debug("Queue overflow, ignoring "+url);
				return;
			}
			queue.add(url);
		}

		public synchronized boolean hasNextURL() {
			return !queue.isEmpty();
		}

		private synchronized URI nextURLOrNull() {
			return queue.poll();
		}

		public synchronized int getConnectionsCount() {
			if (client != null)
				return client.getConnectionsCount();
			return 0;
		}
		
		public synchronized void connect() throws IOException {
			if (client == null)
				client = http.createAsynchronousClient(this);
			
			logger.debug("Launching new connection");
			client.connect(new SessionRequestCallback() {

				public void cancelled(SessionRequest request) {
					errorsCount++;
				}

				public void completed(SessionRequest request) {
					successCount++;						
				}

				public void failed(SessionRequest request) {
					if (request.getException() != null) {
						logger.error("Can not connect: " + request.getException().getMessage());
					}
					errorsCount++;
				}

				public void timeout(SessionRequest request) {
					errorsCount++;
				}
				
			});
		}
		
		public synchronized void shutdown() throws IOException {
			if (client != null) {
				client.shutdown();
				client = null;
			}
		}
		
		public void initalizeContext(final HttpContext context, final Object attachment) {
			context.setAttribute("target", attachment);
			
//			if (base != null && base.getHost() != null)
//				context.setAttribute(ExecutionContext.HTTP_TARGET_HOST, new HttpHost(base.getHost()));
//			else
//				context.setAttribute(ExecutionContext.HTTP_TARGET_HOST, new HttpHost(""));
		}

		public void finalizeContext(final HttpContext context) {
			if (context.getAttribute("url") != null) {
				errorsCount += 1;
				retryURL((URI)context.getAttribute("url"));
			}
		}

		public HttpRequest submitRequest(final HttpContext context) {
			if (interrupted || isExceededErrorThreshold()) {
				return null;
			}
			
			URI url = nextURLOrNull();
			if (url == null) {
				logger.debug("No more requests to submit");
				return null;
			}
			
			String page = url.getRawPath();
			if (page.length() == 0)
				page = "/";
			if (url.getRawQuery() != null) page += "?"+url.getRawQuery();
			
			HttpRequest request = new BasicHttpRequest("GET", page);
			
			request.addHeader("Host", (vhost == null ? http.getLocator().getAddress().toString() : vhost)+":"+http.getLocator().getPort());
			
			context.setAttribute("request", request);
			context.setAttribute("url", url);
			
			return request;
		}

		public void handleResponse(final HttpResponse response, final HttpContext context) {
			successCount += 1;
			try {
				HTTPResponse myResponse = new HTTPResponseAdapter(response, bufferSize);
				
				HttpRequest request = (HttpRequest) context.getAttribute("request");
				context.setAttribute("request", null);
				HTTPRequest myRequest = new HTTPRequestAdapter(request);
				
				int status = myResponse.getStatusCode();
				URI url = (URI)context.getAttribute("url");
				context.setAttribute("url", null);
				
//				WebPageEntity referrer = (WebPageEntity)context.getAttribute("referrer");
//				context.setAttribute("referrer", null);
				
				if (status < 200 || status >= 400) {
					logger.error(request.getRequestLine()+" ->  "+response.getStatusLine().toString());
				} else {
					logger.info(request.getRequestLine()+" ->  "+response.getStatusLine().toString());
				}
				
				String contentType = myResponse.getContentType();
				if (myResponse.getContentType() != null) {
					WebPageEntity pageEntity = null;
					
					if (status == 200)
						pageEntity = factory.createWebPage(realm, spaceId, http.getLocator(), url, contentType);

					if (contentType.matches("(text/|application/x-javascript).*")) {
						String content = new String(myResponse.getContent(bufferSize));
						if (followLinks) {
							for (URI link: getLinks(url, content)) {
								if (interrupted) return;
/*								if (pageEntity != null) {
									// add links
									WebPageEntity linkedPageEntity = factory.createWebPage(realm, spaceId, http.getLocator(), link, null);
									pageEntity.addLink(linkedPageEntity);
								}
*/								follow(link);
							}
							
/*							if (pageEntity != null)
								pageEntity.update();
*/						}
					}
				}

				// redirect
				if (status >= 300) {
					Header locationHeader = response.getFirstHeader("Location");
					if (locationHeader != null) {
						URI location = URI.create(locationHeader.getValue());
						logger.warning("Redirect "+url+" to "+location);
						int port = location.getPort() == -1 ? 80 : location.getPort();
						String hostname = location.getHost();
						List<InternetAddress> addresses;
						if (InternetAddressMatcher.matches(hostname)) {
							addresses = new ArrayList<InternetAddress>(1);
							addresses.add(InternetAddress.fromString(hostname));
						} else {
							addresses = resolver.getAddressesByName(hostname);
						}
						for (InternetAddress address : addresses) {
							factory.createWebSite(realm, spaceId, new TCPSocketLocator(address, port), hostname);
						}
						if (followLinks) {
							follow(url.resolve(location));
						}
					}
				}
				
				// now run all modules
				for (IWebSpiderModule module: modules) {
					try {
						module.handle(getContext(), myRequest, myResponse);
					} catch (OutOfScopeException e) {
						logger.debug("Ignoring "+e.getURL()+" (out of scope)");
					}
				}
			} catch (IOException ex) {
				ex.printStackTrace();
				logger.error("I/O error when handling response: " + ex.getMessage(), ex);
			} catch (Exception ex) {
				ex.printStackTrace();
				logger.error("Error when handling response: " + ex.getMessage(), ex);
/*			} finally {
				try {
					response.getEntity().getContent().close();
				} catch (Exception e) {
					//nothing
				}
*/			}
		}
	}

	private Set<URI> getLinks(URI sourceURL, String content) {
		Set<URI> answer = new HashSet<URI>();
//		String protocolPattern = "[-a-z0-9]+://";
		String protocolPattern = "https?://";
		String hostPattern = "[-a-z0-9]+(\\.[-a-z0-9]+)*";
//		String pathPattern = "[-a-z0-9_:\\@&?=+,.!/~*'%\\$]*";
		String pathPattern = "[-a-z0-9_:\\@&?=+,.!/~*%\\$]*";
		String linkPattern = "("+protocolPattern+")?("+hostPattern+")?"+pathPattern;
		Pattern links = Pattern.compile("(href|src|action)=[\"'\\\\]*("+linkPattern+")[\"'\\\\]*", Pattern.CASE_INSENSITIVE);
		Pattern otherURLs = Pattern.compile("("+protocolPattern+"("+hostPattern+")?"+pathPattern+")", Pattern.CASE_INSENSITIVE);
		
		Matcher matcher = links.matcher(content);
		while (matcher.find()) {
			try {
				answer.add(sourceURL.resolve(matcher.group(2)));
//				System.out.println(url+" -> "+matcher.group(2));
			} catch (IllegalArgumentException e) {
//				System.err.println("Illegal URI: \""+matcher.group(2)+"\"");
			}
		}
		
		matcher = otherURLs.matcher(content);
		while (matcher.find()) {
			try {
				answer.add(sourceURL.resolve(matcher.group(1)));
//				System.out.println(url+" (other) -> "+matcher.group(1));
			} catch (IllegalArgumentException e) {
//				System.err.println("Illegal URI: \""+matcher.group(1)+"\"");
			}
		}

		return answer;
	}

	
	public void setServices(ILogger logger, IWebEntityFactory factory, INameResolver resolver) {
		this.logger = logger;
		logger.enableDebug();
		this.factory = factory;
		this.resolver = resolver;
	}
	
	
	/* Spider API */
	
	public void setRealm(long realm) {
		this.realm = realm;
	}
	
	public void setSpaceId(long spaceId) {
		this.spaceId = spaceId;
	}

	public void setFollowLinks(boolean followLinks) {
		this.followLinks = followLinks;
	}
	
	public boolean getFollowLinks() {
		return followLinks;
	}
	
	public void setFetchImages(boolean fetchImages) {
		this.fetchImages = fetchImages;
	}
	
	public boolean getFetchImages() {
		return fetchImages;
	}

	public void setMaximumConnections(int maximumConnections) {
		this.maximumConnections = maximumConnections;
	}
	
	public int getMaximumConnections() {
		return maximumConnections;
	}
	
	public void setBufferSize(int bufferSize) {
		this.bufferSize = bufferSize;
	}

	public int getBufferSize() {
		return bufferSize;
	}
	
	public void setQueueSize(int queueSize) {
		this.queueSize = queueSize;
	}

	public int getQueueSize() {
		return queueSize;
	}
	
	public synchronized void addModule(IWebSpiderModule module) {
		modules.add(module);
	}

	public synchronized void removeModule(IWebSpiderModule module) {
		modules.remove(module);
	}
	
	public synchronized List<IWebSpiderModule> getModules() {
		return Collections.unmodifiableList(modules);
	}

	public void addTarget(HTTP http, String vhost) {
		addTarget(new WebSite(http, vhost));
	}
	
	public synchronized void addTarget(WebSite target) {
		if (workers.containsKey(target))
			return;

		WebSiteEntity entity = factory.createWebSite(realm, spaceId, target.http.getLocator(), target.vhost);
		entity.addTag("Target");
		entity.addToSpace(spaceId);
		entity.update();
		
		WebSpiderWorker worker = new WebSpiderWorker(target.http, target.vhost);
		workers.put(target, worker);
		
		for (IWebSpiderModule module: modules) {
			try {
				module.start(worker.getContext());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public synchronized void removeTarget(WebSite target) {
		WebSpiderWorker worker = workers.remove(target);
		if (worker != null)
			try {
				worker.shutdown();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}

	public synchronized Set<WebSite> getTargets() {
		return Collections.unmodifiableSet(workers.keySet());
	}
	
	private synchronized void removeAllTargets() {
		workers.clear();
	}
	
	private boolean hasNextURL() {
		for (WebSpiderWorker worker: workers.values()) {
			if (worker.hasNextURL())
				return true;
		}
		return false;
	}
	
	private int getConnectionsCount() {
		int count = 0;
		for (WebSpiderWorker worker: workers.values()) {
			count += worker.getConnectionsCount();
		}
		return count;
	}
	
	public void run() throws InterruptedException, IOException {
		interrupted = false;
		try {
			while (!Thread.currentThread().isInterrupted()) {
				if (!hasNextURL()) {
					logger.debug("No next URL.. waiting..");
					Thread.sleep(2000);
				}
				if (!hasNextURL())
					Thread.sleep(5000);
				if (!hasNextURL())
					Thread.sleep(5000);
				
				if (!hasNextURL()) {
					while (getConnectionsCount() >= maximumConnections && !isExceededErrorThreshold() && !Thread.currentThread().isInterrupted()) {
						logger.debug("Waiting, still "+getConnectionsCount()+" active connections");
						Thread.sleep(1000);
					}
					break;
				}
				
				// XXX gracefully handle filtered web sites
				// for example blogspot or youtube or something from cn
				
				for (WebSpiderWorker worker: workers.values()) {
					if (worker.hasNextURL())
						worker.connect();
				}
				
				Thread.sleep(500);
				
				while (getConnectionsCount() >= maximumConnections && !isExceededErrorThreshold() && !Thread.currentThread().isInterrupted()) {
					logger.debug("Already "+getConnectionsCount()+" connections, sleeping...");
					Thread.sleep(1000);
				}
				
				if (isExceededErrorThreshold()) {
					logger.error("Exceeded maximum number of errors: "+errorsCount+"/"+successCount);
					break;
				}
			}
		} finally {
			for (IWebSpiderModule module: modules) {
				try {
					module.stop();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			if (Thread.currentThread().isInterrupted())
				interrupted = true;
			
			for (WebSpiderWorker worker: workers.values()) {
				worker.shutdown();
			}
			
			removeAllTargets();
		}
	}

	private boolean isExceededErrorThreshold() {
		return errorsCount > ((successCount + 1) * 5);
	}
	
	private synchronized void follow(URI url) {
		try {
			fetch(url, "GET", null, null);
		} catch (OutOfScopeException e) {
			logger.debug("Ignoring "+url+" (out of scope)");
		}
	}

	public synchronized void fetch(URI url, String method, Map<String,String> headers, String content)
			throws OutOfScopeException {
		// TODO (now we just do a GET; method, headers and content are ignored)
		visit(url);
	}
	
	public synchronized void visit(URI url) throws OutOfScopeException {
		url = url.normalize();
		String path = url.getPath();
		if (path == null) {
			logger.debug("Bad URL, null path: "+url);
			return; // bad url, like javascript:void
		}
		String host = url.getHost();
		if (host == null) {
			logger.debug("Bad URL, null host: "+url);
			return; // bad url, like javascript:void
		}
		if (!fetchImages && path.matches(".*(jpg|gif|png)$"))
			return;

		for (WebSpiderWorker worker: workers.values()) {
			int basePort = worker.base.getPort() == -1 ? 80 : worker.base.getPort();
			int urlPort = url.getPort() == -1 ? 80 : url.getPort();
			if (host.equals(worker.base.getHost()) && basePort == urlPort) {
				System.out.println("found web spider worker for "+url);
				worker.addURL(url);
				return;
			}
		}
		System.out.println("not found web spider worker for "+url);
		throw new OutOfScopeException(url);
	}
}
