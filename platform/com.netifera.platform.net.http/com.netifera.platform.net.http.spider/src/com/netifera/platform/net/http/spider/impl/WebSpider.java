package com.netifera.platform.net.http.spider.impl;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
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

import com.netifera.platform.api.log.ILogManager;
import com.netifera.platform.api.log.ILogger;
import com.netifera.platform.net.http.internal.spider.Activator;
import com.netifera.platform.net.http.service.AsynchronousHTTPClient;
import com.netifera.platform.net.http.service.HTTP;
import com.netifera.platform.net.http.spider.HTTPRequest;
import com.netifera.platform.net.http.spider.HTTPResponse;
import com.netifera.platform.net.http.spider.IWebSpider;
import com.netifera.platform.net.http.spider.IWebSpiderContext;
import com.netifera.platform.net.http.spider.IWebSpiderModule;
import com.netifera.platform.net.http.web.model.WebPageEntity;
import com.netifera.platform.util.addresses.inet.InternetAddress;
import com.netifera.platform.util.locators.TCPSocketLocator;
import com.netifera.platform.util.patternmatching.InternetAddressMatcher;

public class WebSpider implements IWebSpider {
	final private HTTP http;
	private boolean followLinks = true;
	private boolean fetchImages = false;
	private int maximumConnections = 5;
	private int bufferSize = 1024*16;
	private URI base;// = URI.create("http:///");
	private String hostname = null;
	private final BloomFilter knownPaths = new BloomFilter(1024*1024); // 1M
	private final Queue<URI> urlsQueue = new LinkedList<URI>();
	private int queueSize = 100;
	private long realm;
	private long spaceId;

	private volatile int successCount = 0;
	private volatile int errorsCount = 0;
	private volatile boolean interrupted = false;
	
	private List<IWebSpiderModule> modules = new ArrayList<IWebSpiderModule>();

	private ILogger logger;


	public WebSpider(HTTP service) {
		this.http = service;
	}
	
	public synchronized void addModule(IWebSpiderModule module) {
		modules.add(module);
	}

	private IWebSpiderContext getContext() {
		return new IWebSpiderContext() {
			public TCPSocketLocator getLocator() {
				return http.getLocator();
			}

			public URI getBaseURL() {
				return WebSpider.this.getBaseURL();
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
		};
	}
	
	class WebSpiderWorker implements HttpRequestExecutionHandler {
		private URI url = null;

		public void initalizeContext(final HttpContext context, final Object attachment) {
//			if (base != null && base.getHost() != null)
//				context.setAttribute(ExecutionContext.HTTP_TARGET_HOST, new HttpHost(base.getHost()));
//			else
//				context.setAttribute(ExecutionContext.HTTP_TARGET_HOST, new HttpHost(""));
		}

		public void finalizeContext(final HttpContext context) {
			if (context.getAttribute("url")!=null) {
				errorsCount += 1;
				retryURL((URI)context.getAttribute("url"));
			}
		}

		public HttpRequest submitRequest(final HttpContext context) {
			if (interrupted || isExceededErrorThreshold()) {
				return null;
			}
			
			url = nextURLOrNull();
			if (url == null) {
				logger.debug("No more requests to submit");
				return null; // no new request to submit
			}
			
			String page = url.getRawPath();
			if (page.length() == 0)
				page = "/";
			if (url.getRawQuery() != null) page += "?"+url.getRawQuery();
			
			HttpRequest request = new BasicHttpRequest("GET", page);
			
			request.addHeader("Host", http.getURIHostPort(hostname));
			
			context.setAttribute("request", request);
			context.setAttribute("url", url);
//			context.setAttribute("referrer", link.referrer);
			
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
					
					if (status == 200) {
						pageEntity = Activator.getInstance().getWebEntityFactory().createWebPage(realm, toolContext.getSpaceId(), http.getLocator(), url, contentType);
/*						if (referrer != null) {
							referrer.addLink(pageEntity);
							referrer.update();
						}
*/					}

					if (contentType.matches("(text/|application/x-javascript).*")) {
						String content = new String(myResponse.getContent(bufferSize));
//						WebPage page = new WebPage(url, content);
						if (followLinks) {
							for (URI link: getLinks(url, content)) {
								if (interrupted) return;
								follow(link, pageEntity);
							}
						}
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
							addresses = Activator.getInstance().getNameResolver().getAddressesByName(hostname);
						}
						for (InternetAddress address : addresses) {
							Activator.getInstance().getWebEntityFactory().createWebSite(realm, spaceId, new TCPSocketLocator(address, port), hostname);
						}
						if (followLinks)
							follow(url.resolve(location), null);
					}
				}
				
				// now run all modules
				for (IWebSpiderModule module: modules) {
					module.handle(getContext(), myRequest, myResponse);
				}
			} catch (IOException ex) {
				logger.error("I/O error when handling response: " + ex.getMessage(), ex);
			} catch (Exception ex) {
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
	
	public void setRealm(long realm) {
		this.realm = realm;
	}
	
	public void setSpaceId(long spaceId) {
		this.spaceId = spaceId;
	}

	public void setBaseURL(URI base) {
		this.base = base;
		
		if (hostname == null && base.getHost() != null && base.getHost().length()>0)
			hostname = base.getHost();
	}
	
	public URI getBaseURL() {
		return base;
	}
	
	public void setHostName(String hostname) {
		this.hostname = hostname;
		this.base = URI.create(http.getURI(hostname));
	}
	
	public void setFollowLinks(boolean followLinks) {
		this.followLinks = followLinks;
	}
	
	public void setFetchImages(boolean fetchImages) {
		this.fetchImages = fetchImages;
	}

	public void setMaximumConnections(int maximumConnections) {
		this.maximumConnections = maximumConnections;
	}
	
	public void setBufferSize(int bufferSize) {
		this.bufferSize = bufferSize;
	}
	
	public void setQueueSize(int queueSize) {
		this.queueSize = queueSize;
	}
	
	public void run() throws InterruptedException, IOException {
		interrupted = false;
		final AsynchronousHTTPClient client = http.createAsynchronousClient(new WebSpiderWorker());
		try {
			for (IWebSpiderModule module: modules) {
				try {
					module.start(getContext());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
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
					while (client.getConnectionsCount() >= maximumConnections && !isExceededErrorThreshold() && !Thread.currentThread().isInterrupted()) {
						logger.debug("Waiting, still "+client.getConnectionsCount()+" active connections");
						Thread.sleep(1000);
					}
					break;
				}
				
				// XXX gracefully handle filtered web sites
				// for example blogspot or youtube or something from cn
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
				
				Thread.sleep(500);
				
				while (client.getConnectionsCount() >= maximumConnections && !isExceededErrorThreshold() && !Thread.currentThread().isInterrupted()) {
					logger.debug("Waiting, currently already "+client.getConnectionsCount()+" connections");
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
			client.shutdown();
		}
	}

	private boolean isExceededErrorThreshold() {
		return errorsCount > ((successCount + 1) * 5);
	}
	
	private synchronized boolean hasNextURL() {
		return !urlsQueue.isEmpty();
	}

	private synchronized URI nextURLOrNull() {
		return urlsQueue.poll();
	}

	private synchronized void follow(URI url, WebPageEntity referrer) {
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
		// TODO improve "outside site" concept
		int basePort = base.getPort() == -1 ? 80 : base.getPort();
		int urlPort = url.getPort() == -1 ? 80 : url.getPort();
		// follow redirects only to subdomains
		if (!host.equals(base.getHost()) || basePort != urlPort) {
			logger.debug("Ignoring "+url+" (outside site)");
//			String site = url.resolve("/").toString();
			//TODO
			return;
		}
		addURL(url);
	}

	public synchronized void addURL(URI url) {
		if (urlsQueue.size() > queueSize) {
//			toolContext.debug("Queue overflow, ignoring "+url);
			return;
		}
		
		String path = url.getPath();
		if (path.length() == 0)
			path = "/";
		//FIXME what if we want to send again the same request with different query parameters?
		if (knownPaths.add(path)) // if returns true, it means it was already added to the queue before, already crawled or enqueued
			return;
		urlsQueue.add(url);
	}
	
	private synchronized void retryURL(URI url) {
		logger.warning("Retrying "+url);
		if (urlsQueue.size() > queueSize) {
//			toolContext.debug("Queue overflow, ignoring "+url);
			return;
		}
		urlsQueue.add(url);
	}

	public void get(URI url) {
		addURL(url);
	}
	
	protected void setLogManager(ILogManager logManager) {
		logger = logManager.getLogger("Sniffing Daemon");
	}
	
	protected void unsetLogManager(ILogManager logManager) {
		
	}
}
