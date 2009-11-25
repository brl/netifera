package com.netifera.platform.net.dns.tools;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.xbill.DNS.AAAARecord;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.NSRecord;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

import com.netifera.platform.api.probe.IProbe;
import com.netifera.platform.api.tools.ITool;
import com.netifera.platform.api.tools.IToolContext;
import com.netifera.platform.api.tools.ToolException;
import com.netifera.platform.net.dns.internal.tools.Activator;
import com.netifera.platform.net.dns.service.DNS;
import com.netifera.platform.net.dns.service.client.AsynchronousLookup;
import com.netifera.platform.net.dns.service.nameresolver.INameResolver;
import com.netifera.platform.tools.RequiredOptionMissingException;
import com.netifera.platform.util.addresses.inet.IPv4Address;
import com.netifera.platform.util.addresses.inet.IPv6Address;
import com.netifera.platform.util.addresses.inet.InternetAddress;
import com.netifera.platform.util.asynchronous.CompletionHandler;

public class HostNamesBruteforcer implements ITool {
	private final static boolean DEBUG = false;
	private static final int DEFAULT_DNS_INTERVAL = 200; // milliseconds between requests

	private DNS dns;
	private Name domain;
	private INameResolver resolver;
	
	private IToolContext context;
	private long realm;

	private InternetAddress ignoreAddress = null;
	private boolean tryTLDs = false;

	private AtomicInteger activeRequests;
	
	private static String[] hostNames = {"www", "www2", "web", "ssl", "static", "main", "home", "go",
			"ftp", "image", "images", "photo", "photos", "img", "pictures", "search",
			"mail", "webmail", "email", "mymail", "mx", "snmp", "pop", "pop3", "imap", "exchange",
			"ns", "dns", "mdns", "nameserver", "fw", "firewall", "router",
			"register", "login", "id", "passport",
			"vpn", "proxy", "cache", "upload", "download",
			"private", "partner", "partners", "customer", "customers", "member", "members", "user", "users",
			"b2b", "erp",
			"global", "usa", "us", "europe", "asia",
			"forum", "forums", "bbs", "blog", "blogs", "weblog", "weblogs", "wiki", "media", "video", "videos", "movie", "movies", "music", "tv", "community",
			"support", "network", "admin", "security", "secure", "sec", "manager", "manage", "management",
			"news", "feeds", "service", "sevices", "game", "games", "help", "list", "lists", "archive", "archives",
			"file", "files", "database", "data", "db", "oracle", "sql", "mysql", "cvs", "svn", "irc", "dc", "domain", "update",
			"buy", "sell", "sells", "pay", "payment", "payments", "shop", "shopping", "store", "webstore", "products", "product", "order", "orders", "report", "reports", "test",
			"research", "job", "jobs", "careers",
			"sms", "mobile", "phone",
			"stats", "localhost"
	};

	private static String[] gTLDs = {"com", "org", "net", "edu", /*"gov", "mil", "aero",*/ "biz", "info"};
	private static String[] ccTLDs = {"ac", "ad", "ae", "af", "ag", "ai", "al", "am", "an", "ao", "aq", "ar", "as", "at", "au", "aw", "ax", "az", "ba", "bb", "bd", "be", "bf", "bg", "bh", "bi", "bj", "bm", "bn", "bo", "br", "bs", "bt", "bw", "by", "bz", "ca", "cc", "cd", "cf", "cg", "ch", "ci", "ck", "cl", "cm", "cn", "co", "cr", "cu", "cv", "cx", "cy", "cz", "de", "dj", "dk", "dm", "do", "dz", "ec", "ee", "eg", "er", "es", "et", "eu", "fi", "fj", "fk", "fm", "fo", "fr", "ga", "gd", "ge", "gf", "gg", "gh", "gi", "gl", "gm", "gn", "gp", "gq", "gr", "gs", "gt", "gu", "gw", "gy", "hk", "hm", "hn", "hr", "ht", "hu", "id", "ie", "il", "im", "in", "io", "iq", "ir", "is", "it", "je", "jm", "jo", "jp", "ke", "kg", "kh", "ki", "km", "kn", "kp", "kr", "kw", "ky", "kz", "la", "lb", "lc", "li", "lk", "lr", "ls", "lt", "lu", "lv", "ly", "ma", "mc", "md", "me", "mg", "mh", "mk", "ml", "mm", "mn", "mo", "mp", "mq", "mr", "ms", "mt", "mu", "mv", "mw", "mx", "my", "mz", "na", "nc", "ne", "nf", "ng", "ni", "nl", "no", "np", "nr", "nu", "nz", "om", "pa", "pe", "pf", "pg", "ph", "pk", "pl", "pn", "pr", "ps", "pt", "pw", "py", "qa", "re", "ro", "rs", "ru", "rw", "sa", "sb", "sc", "sd", "se", "sg", "sh", "si", "sk", "sl", "sm", "sn", "sr", "st", "su", "sv", "sy", "sz", "tc", "td", "tf", "tg", "th", "tj", "tk", "tl", "tm", "tn", "to", "tr", "tt", "tv", "tw", "tz", "ua", "ug", "uk", "us", "uy", "uz", "va", "vc", "ve", "vg", "vi", "vn", "vu", "wf", "ws", "ye", "za", "zm", "zw"};
	private static String[] countryCodePrefixes = {"co", "com", "net", "org", "ac", "edu"};
	
	public void toolRun(IToolContext context) throws ToolException {
		this.context = context;
		
		final int sendDelay = getSendDelay();

		// XXX hardcode local probe as realm
		IProbe probe = Activator.getInstance().getProbeManager().getLocalProbe();
		realm = probe.getEntity().getId();
		
		setupToolOptions();

		context.setTitle("Bruteforce host names *."+domain);
		
		int totalWork = hostNames.length + ccTLDs.length;
		if (tryTLDs)
			totalWork += gTLDs.length + ccTLDs.length * (countryCodePrefixes.length+1);
		context.setTotalWork(totalWork);

		if (dns != null)
			try {
				resolver = dns.createNameResolver(Activator.getInstance().getSocketEngine());
			} catch (IOException e) {
				context.exception("I/O Exception", e);
				context.done();
				return;
			}
		else
			resolver = Activator.getInstance().getNameResolver();

		context.setStatus("Get authoritative name servers");
		
		if (!getNS()) {
			context.error("Authoritative name servers not found, "+domain+" might not be a real domain");
			context.done();
			return;
		}

		context.setStatus("Lookup *."+domain);

		try {

			try {
				Random random = new Random();
				random.setSeed(System.currentTimeMillis());
				String randomName = "";
				String alphabet = "abcdefghijklmnopqrstuvwxyz";
				int length = random.nextInt(10)+5;
				for (int i=0; i<length; i++)
					randomName += alphabet.charAt(random.nextInt(alphabet.length()));
				ignoreAddress = resolver.getAddressByName(randomName+"."+domain.toString());
			} catch (UnknownHostException e) {
			}

			if (ignoreAddress != null)
				context.warning("The DNS server resolves non-existent names");

			activeRequests = new AtomicInteger(0);
			resolve(domain.toString());
			for (String each :hostNames) {
				resolve(each + "." + domain.toString());
				Thread.sleep(sendDelay);
			}
			for (String each :ccTLDs) {
				resolve(each + "." + domain.toString());
				Thread.sleep(sendDelay);
			}

			if (tryTLDs) {
				String name = domain.toString();
				name = name.substring(0, name.lastIndexOf("."));
				
				context.setStatus("Lookup "+name+".*");

				for (String tld: gTLDs) {
					resolve(name + "." + tld);
					Thread.sleep(sendDelay);
				}
				
				for (String cc: ccTLDs) {
					resolve(name + "." + cc);
					for (String prefix: countryCodePrefixes) {
						Thread.sleep(sendDelay);
						resolve(name + "." + prefix + "." + cc);
					}
				}
			}
			
			while (activeRequests.get() > 0) {
				if(DEBUG) {
					context.debug("activeRequests = "+activeRequests.get());
				}
				Thread.sleep(500);
			}
		} catch (InterruptedException e) {
			context.warning("Interrupted");
		} finally {
			try {
				if (dns != null)
					resolver.shutdown();
			} catch (IOException e) {
				context.exception("I/O Exception", e);
			};
			context.done();
		}
	}

	// TODO only bruteforcing Type.A, what about Type.AAAA?
	private void resolve(final String fqdm) {
		try {
			final AsynchronousLookup lookup = new AsynchronousLookup(fqdm);
			lookup.setResolver(resolver.getExtendedResolver());
			CompletionHandler<Record[],Void> handler = new CompletionHandler<Record[],Void>() {
				int retries = 0;
				public void cancelled(Void attachment) {
					activeRequests.decrementAndGet();
					context.warning(fqdm + " lookup cancelled");
					context.worked(1);
				}
				public void completed(Record[] result, Void attachment) {
					if (lookup.getResult() == AsynchronousLookup.TRY_AGAIN && retries < 3) {
						retries = retries + 1;
						context.debug("Retrying: "+fqdm+" ("+retries+")");
						lookup.run(attachment, this);
						return;
					}
					activeRequests.decrementAndGet();
					context.worked(1);
					if (result == null) {
						context.error(fqdm+" lookup failed: "+lookup.getErrorString());
						return;
					}
					for (Record record: result)
						processRecord(record);
				}
				public void failed(Throwable exc, Void attachment) {
					activeRequests.decrementAndGet();
					context.worked(1);
					if(lookup.getResult() == AsynchronousLookup.HOST_NOT_FOUND ||
							lookup.getResult() == AsynchronousLookup.TYPE_NOT_FOUND) {
						return;
					}
					if(exc instanceof SocketTimeoutException) {
						context.warning("Timeout looking up " + fqdm);
						return;
					}
					context.exception(fqdm+" lookup failed", exc);
				}};
				
			activeRequests.incrementAndGet();
			lookup.run(null,handler);
		} catch (TextParseException e) {
			context.warning("Malformed host name: " + fqdm);
		}
	}

	private void processRecord(Record record) {
		if (record instanceof ARecord) {
			IPv4Address addr = IPv4Address.fromInetAddress(((ARecord)record).getAddress());
			if (addr != null) {
				if (ignoreAddress == null || !ignoreAddress.equals(addr)) {
					context.info(record.toString());
					Activator.getInstance().getDomainEntityFactory().createARecord(realm, context.getSpaceId(), ((ARecord)record).getName().toString(), addr);
				}
			}
		} else if (record instanceof AAAARecord) {
			IPv6Address addr = IPv6Address.fromInetAddress(((AAAARecord)record).getAddress());
			if (addr != null) {
				if (ignoreAddress == null || !ignoreAddress.equals(addr)) {
					context.info(record.toString());
					Activator.getInstance().getDomainEntityFactory().createAAAARecord(realm, context.getSpaceId(), ((AAAARecord)record).getName().toString(), addr);
				}
			}
		}
	}
	
	private boolean getNS() {
		Lookup lookup = new Lookup(domain, Type.NS);
		lookup.setResolver(resolver.getExtendedResolver());
		lookup.setSearchPath((Name[])null);

		Record[] records = lookup.run();
		if (records == null) {
			context.info("No NS records found for " + domain);
			return false;
		}
		// if the domain has ns records, it exists
		Activator.getInstance().getDomainEntityFactory().createDomain(realm, context.getSpaceId(), domain.toString());
		for (int i = 0; i < records.length; i++) {
			NSRecord ns = (NSRecord) records[i];
			Activator.getInstance().getDomainEntityFactory().createNSRecord(realm, context.getSpaceId(), domain.toString(), ns.getTarget().toString());
		}
		return true;
	}

	private void setupToolOptions() throws ToolException {
		dns = (DNS) context.getConfiguration().get("dns");
		String domainString = (String) context.getConfiguration().get("domain");
		if (domainString == null || domainString.length() == 0)
			throw new RequiredOptionMissingException("domain");
		if (domainString.endsWith(".")) {
			domainString = domainString.substring(0, domainString.length()-1);
		}
		try {
			domain = new Name(domainString);
		} catch (TextParseException e) {
			throw new ToolException("Malformed domain name: '"+domainString+"'", e);
		}
		
		Boolean tryTLDs = (Boolean) context.getConfiguration().get("tryTLDs");
		this.tryTLDs = tryTLDs == true;
	}
	
	private int getSendDelay() {
		final String property = System.getProperty("netifera.dns.delay");
		if(property == null) {
			return DEFAULT_DNS_INTERVAL;
		}
		try {
			final int delay = Integer.parseInt(property);
			if(delay < 0 || delay > 10000) {
				return DEFAULT_DNS_INTERVAL;
			}
			return delay;
		} catch(NumberFormatException e) {
			return DEFAULT_DNS_INTERVAL;
		}
	}
}
