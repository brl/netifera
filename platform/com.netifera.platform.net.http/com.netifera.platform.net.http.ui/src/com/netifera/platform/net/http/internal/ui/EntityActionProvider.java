package com.netifera.platform.net.http.internal.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.action.IAction;

import com.netifera.platform.api.model.IShadowEntity;
import com.netifera.platform.net.http.service.HTTP;
import com.netifera.platform.net.http.tools.HTTPBasicAuthBruteforcer;
import com.netifera.platform.net.http.tools.WebApplicationScanner;
import com.netifera.platform.net.http.tools.WebCrawler;
import com.netifera.platform.net.http.web.model.BasicAuthenticationEntity;
import com.netifera.platform.net.http.web.model.WebPageEntity;
import com.netifera.platform.net.http.web.model.WebSiteEntity;
import com.netifera.platform.net.model.ServiceEntity;
import com.netifera.platform.net.wordlists.IWordList;
import com.netifera.platform.tools.options.BooleanOption;
import com.netifera.platform.tools.options.GenericOption;
import com.netifera.platform.tools.options.IntegerOption;
import com.netifera.platform.tools.options.MultipleStringOption;
import com.netifera.platform.tools.options.StringOption;
import com.netifera.platform.ui.actions.ToolAction;
import com.netifera.platform.ui.api.actions.IEntityActionProvider;

public class EntityActionProvider implements IEntityActionProvider {

	private List<IWordList> wordlists = new ArrayList<IWordList>();

	public List<IAction> getActions(IShadowEntity entity) {
		List<IAction> answer = new ArrayList<IAction>();
		
		HTTP http = (HTTP) entity.getAdapter(HTTP.class);
		if (http != null) {
			if (entity instanceof ServiceEntity) {
				Set<String> names = ((ServiceEntity)entity).getAddress().getNames();
				if (names.isEmpty()) {
					addWebCrawler("Crawl web site", answer, http, http.getURI());
					addWebApplicationScanner("Scan for web applications", answer, http, null);
				} else {
					for (String vhost: names) {
						addWebCrawler("Crawl web site " + vhost, answer, http, http.getURI(vhost));
					}
					for (String vhost: names) {
						addWebApplicationScanner("Scan for web applications at " + vhost, answer, http, vhost);
					}
				}
			}
		} else if (entity instanceof WebPageEntity) {
			WebPageEntity page = (WebPageEntity) entity;
			http = (HTTP) page.getWebSite().getHTTP().getAdapter(HTTP.class);

			addWebCrawler("Crawl web site starting at "+page.getPath(), answer, http, page.getURL());
			
			if (page.getAuthentication() instanceof BasicAuthenticationEntity) {
				ToolAction bruteforcer = new ToolAction("Bruteforce authentication", HTTPBasicAuthBruteforcer.class.getName());
				bruteforcer.addFixedOption(new GenericOption(HTTP.class, "target", "Target", "Target HTTP service", http));
				bruteforcer.addOption(new StringOption("hostname", "Host name", "Host name for the web site", page.getWebSite().getVirtualHostName()));
				bruteforcer.addOption(new StringOption("path", "Path", "Path that requires authentication", page.getPath()));
				bruteforcer.addOption(new StringOption("method", "Method", "GET/POST", "GET"));
//				bruteforcer.addOption(new IterableOption(UsernameAndPassword.class, "credentials", "Credentials", "List of credentials to try", null));
				bruteforcer.addOption(new StringOption("usernames", "Usernames", "List of usernames to try, separated by space or comma", "Usernames", null, true));
				bruteforcer.addOption(new MultipleStringOption("usernames_wordlists", "Usernames Wordlists", "Wordlists to try as usernames", "Usernames", getAvailableWordLists(new String[] {IWordList.CATEGORY_USERNAMES, IWordList.CATEGORY_NAMES})));
				bruteforcer.addOption(new StringOption("passwords", "Passwords", "List of passwords to try, separated by space or comma", "Passwords", null, true));
				bruteforcer.addOption(new MultipleStringOption("passwords_wordlists", "Passwords Wordlists", "Wordlists to try as passwords", "Passwords", getAvailableWordLists(new String[] {IWordList.CATEGORY_PASSWORDS, IWordList.CATEGORY_NAMES})));
				bruteforcer.addOption(new BooleanOption("tryNullPassword", "Try null password", "Try null password", true));
				bruteforcer.addOption(new BooleanOption("tryUsernameAsPassword", "Try username as password", "Try username as password", true));
				bruteforcer.addOption(new BooleanOption("singleMode", "Single mode", "Stop after one credential is found", true));
				bruteforcer.addOption(new IntegerOption("maximumConnections", "Maximum connections", "Maximum number of simultaneous connections", 10));
				answer.add(bruteforcer);
			}
		} else if (entity instanceof WebSiteEntity) {
			WebSiteEntity site = (WebSiteEntity) entity;
			http = (HTTP) site.getHTTP().getAdapter(HTTP.class);
			addWebCrawler("Crawl web site", answer, http, site.getRootURL());
			addWebApplicationScanner("Scan for web applications", answer, http, site.getVirtualHostName());
		}
		
		return answer;
	}
	
	private void addWebCrawler(String name, List<IAction> answer, HTTP http, String startURL) {
		// Visit every page of the web server, starting from the given page.
		ToolAction webCrawler = new ToolAction(name, WebCrawler.class.getName());
		webCrawler.addFixedOption(new GenericOption(HTTP.class, "target", "Target", "Target HTTP service", http));
		webCrawler.addOption(new StringOption("url", "Base URL", "URL to start to crawl from", startURL));
		webCrawler.addOption(new BooleanOption("followLinks", "Follow links", "Follow links inside this website?", true));
		webCrawler.addOption(new BooleanOption("fetchImages", "Fetch images", "Fetch images following <img> tags?", false));
		webCrawler.addOption(new BooleanOption("scanWebApplications", "Scan common web applications", "Try common URLs for known web applications?", false));
		webCrawler.addOption(new IntegerOption("maximumConnections", "Maximum connections", "Maximum number of simultaneous connections", 10));
		webCrawler.addOption(new IntegerOption("bufferSize", "Buffer size", "Maximum bytes to download for each page", 1024*16));
		answer.add(webCrawler);
	}
	
	private void addWebApplicationScanner(String name, List<IAction> answer, HTTP http, String hostname) {
		// Attempt to detect web applications
		ToolAction webApplicationScanner = new ToolAction(name, WebApplicationScanner.class.getName());
		webApplicationScanner.addFixedOption(new GenericOption(HTTP.class, "target", "Target", "Target HTTP service", http));
		webApplicationScanner.addOption(new StringOption("hostname", "Host name", "Host name for the web site", hostname != null ? hostname : http.getURIHost()));
		webApplicationScanner.addOption(new IntegerOption("maximumConnections", "Maximum connections", "Maximum number of simultaneous connections", 10));
		webApplicationScanner.addOption(new IntegerOption("bufferSize", "Buffer size", "Maximum bytes to download for each page", 1024*16));
		answer.add(webApplicationScanner);
	}

	public List<IAction> getQuickActions(IShadowEntity shadow) {
		// TODO Auto-generated method stub
		return null;
	}
	
	private String[] getAvailableWordLists(String[] categories) {
		List<String> names = new ArrayList<String>();
		for (IWordList wordlist: wordlists) {
			for (String category: categories) {
				if (wordlist.getCategory().equals(category)) {
					names.add(wordlist.getName());
					break;
				}
			}
		}
		return names.toArray(new String[names.size()]);
	}
	
	protected void registerWordList(IWordList wordlist) {
		this.wordlists.add(wordlist);
	}
	
	protected void unregisterWordList(IWordList wordlist) {
		this.wordlists.remove(wordlist);
	}
}
