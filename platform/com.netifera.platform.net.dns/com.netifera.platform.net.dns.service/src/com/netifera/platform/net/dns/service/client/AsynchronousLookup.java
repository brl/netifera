// Copyright (c) 2002-2004 Brian Wellington (bwelling@xbill.org)

package com.netifera.platform.net.dns.service.client;

import java.io.InterruptedIOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.xbill.DNS.CNAMERecord;
import org.xbill.DNS.Cache;
import org.xbill.DNS.Credibility;
import org.xbill.DNS.DClass;
import org.xbill.DNS.DNAMERecord;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.NameTooLongException;
import org.xbill.DNS.Options;
import org.xbill.DNS.RRset;
import org.xbill.DNS.Rcode;
import org.xbill.DNS.Record;
import org.xbill.DNS.Resolver;
import org.xbill.DNS.ResolverConfig;
import org.xbill.DNS.ResolverListener;
import org.xbill.DNS.SetResponse;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

import com.netifera.platform.util.asynchronous.CompletionHandler;

/**
 * The Lookup object issues queries to caching DNS servers. The input consists
 * of a name, an optional type, and an optional class. Caching is enabled by
 * default and used when possible to reduce the number of DNS requests. A
 * Resolver, which defaults to an ExtendedResolver initialized with the
 * resolvers located by the ResolverConfig class, performs the queries. A search
 * path of domain suffixes is used to resolve relative names, and is also
 * determined by the ResolverConfig class.
 * 
 * A Lookup object may be reused, but should not be used by multiple threads.
 * 
 * @see Cache
 * @see Resolver
 * @see ResolverConfig
 * 
 * @author Brian Wellington
 */

public final class AsynchronousLookup {

	private static Resolver defaultResolver;
	private static Name[] defaultSearchPath;
	private static Map<Integer, Cache> defaultCaches = new HashMap<Integer,Cache>();

	private Resolver resolver;
	private Name[] searchPath;
	private Cache cache;
	private boolean temporary_cache;
	private int credibility;
	private Name name;
	private int type;
	private int dclass;
	private boolean verbose;
	private int iterations;
	private boolean foundAlias;
	private boolean done;
	private boolean doneCurrent;
	private List<Name> aliases;
	private Record[] answers;
	private int result;
	private String error;
	private boolean nxdomain;
	private boolean badresponse;
	private String badresponse_error;
	private boolean networkerror;
	private boolean timedout;
	private boolean nametoolong;
	private boolean referral;

	private static final Name[] noAliases = new Name[0];

	/** The lookup was successful. */
	public static final int SUCCESSFUL = 0;

	/**
	 * The lookup failed due to a data or server error. Repeating the lookup
	 * would not be helpful.
	 */
	public static final int UNRECOVERABLE = 1;

	/**
	 * The lookup failed due to a network error. Repeating the lookup may be
	 * helpful.
	 */
	public static final int TRY_AGAIN = 2;

	/** The host does not exist. */
	public static final int HOST_NOT_FOUND = 3;

	/** The host exists, but has no records associated with the queried type. */
	public static final int TYPE_NOT_FOUND = 4;

	/**
	 * Gets the Resolver that will be used as the default by future Lookups.
	 * 
	 * @return The default resolver.
	 */
	public static synchronized Resolver getDefaultResolver() {
		return defaultResolver;
	}

	/**
	 * Sets the default Resolver to be used as the default by future Lookups.
	 * 
	 * @param resolver
	 *            The default resolver.
	 */
	public static synchronized void setDefaultResolver(Resolver resolver) {
		defaultResolver = resolver;
	}

	/**
	 * Gets the Cache that will be used as the default for the specified class
	 * by future Lookups.
	 * 
	 * @param dclass
	 *            The class whose cache is being retrieved.
	 * @return The default cache for the specified class.
	 */
	public static synchronized Cache getDefaultCache(int dclass) {
		// DClass.check(dclass);
		Cache c = defaultCaches.get(dclass);
		if (c == null) {
			c = new Cache(dclass);
			defaultCaches.put(dclass, c);
		}
		return c;
	}

	/**
	 * Sets the Cache to be used as the default for the specified class by
	 * future Lookups.
	 * 
	 * @param cache
	 *            The default cache for the specified class.
	 * @param dclass
	 *            The class whose cache is being set.
	 */
	public static synchronized void setDefaultCache(Cache cache, int dclass) {
		// DClass.check(dclass);
		defaultCaches.put(dclass, cache);
	}

	/**
	 * Gets the search path that will be used as the default by future Lookups.
	 * 
	 * @return The default search path.
	 */
	public static synchronized Name[] getDefaultSearchPath() {
		return defaultSearchPath;
	}

	/**
	 * Sets the search path to be used as the default by future Lookups.
	 * 
	 * @param domains
	 *            The default search path.
	 */
	public static synchronized void setDefaultSearchPath(Name[] domains) {
		defaultSearchPath = domains;
	}

	/**
	 * Sets the search path that will be used as the default by future Lookups.
	 * 
	 * @param domains
	 *            The default search path.
	 * @throws TextParseException
	 *             A name in the array is not a valid DNS name.
	 */
	public static synchronized void setDefaultSearchPath(String[] domains)
			throws TextParseException {
		if (domains == null) {
			defaultSearchPath = null;
			return;
		}
		Name[] newdomains = new Name[domains.length];
		for (int i = 0; i < domains.length; i++)
			newdomains[i] = Name.fromString(domains[i], Name.root);
		defaultSearchPath = newdomains;
	}

	private final void reset() {
		iterations = 0;
		foundAlias = false;
		done = false;
		doneCurrent = false;
		aliases = null;
		answers = null;
		result = -1;
		error = null;
		nxdomain = false;
		badresponse = false;
		badresponse_error = null;
		networkerror = false;
		timedout = false;
		nametoolong = false;
		referral = false;
		if (temporary_cache)
			cache.clearCache();
	}

	/**
	 * Create a Lookup object that will find records of the given name, type,
	 * and class. The lookup will use the default cache, resolver, and search
	 * path, and look for records that are reasonably credible.
	 * 
	 * @param name
	 *            The name of the desired records
	 * @param type
	 *            The type of the desired records
	 * @param dclass
	 *            The class of the desired records
	 * @throws IllegalArgumentException
	 *             The type is a meta type other than ANY.
	 * @see Cache
	 * @see Resolver
	 * @see Credibility
	 * @see Name
	 * @see Type
	 * @see DClass
	 */
	public AsynchronousLookup(Name name, int type, int dclass) {
		// Type.check(type);
		// DClass.check(dclass);
		if (!Type.isRR(type) && type != Type.ANY) {
			System.err.println("AsynchronousLookup error: Cannot query for meta-types other than ANY");
			throw new IllegalArgumentException("Cannot query for meta-types other than ANY");
		}
		this.name = name;
		this.type = type;
		this.dclass = dclass;
		synchronized (AsynchronousLookup.class) {
			this.resolver = getDefaultResolver();
			this.searchPath = getDefaultSearchPath();
			this.cache = getDefaultCache(dclass);
		}
		this.credibility = Credibility.NORMAL;
		this.verbose = Options.check("verbose");
		this.result = -1;
	}

	/**
	 * Create a Lookup object that will find records of the given name and type
	 * in the IN class.
	 * 
	 * @param name
	 *            The name of the desired records
	 * @param type
	 *            The type of the desired records
	 * @throws IllegalArgumentException
	 *             The type is a meta type other than ANY.
	 * @see #Lookup(Name,int,int)
	 */
	public AsynchronousLookup(Name name, int type) {
		this(name, type, DClass.IN);
	}

	/**
	 * Create a Lookup object that will find records of type A at the given name
	 * in the IN class.
	 * 
	 * @param name
	 *            The name of the desired records
	 * @see #Lookup(Name,int,int)
	 */
	public AsynchronousLookup(Name name) {
		this(name, Type.A, DClass.IN);
	}

	/**
	 * Create a Lookup object that will find records of the given name, type,
	 * and class.
	 * 
	 * @param name
	 *            The name of the desired records
	 * @param type
	 *            The type of the desired records
	 * @param dclass
	 *            The class of the desired records
	 * @throws TextParseException
	 *             The name is not a valid DNS name
	 * @throws IllegalArgumentException
	 *             The type is a meta type other than ANY.
	 * @see #Lookup(Name,int,int)
	 */
	public AsynchronousLookup(String name, int type, int dclass)
			throws TextParseException {
		this(Name.fromString(name), type, dclass);
	}

	/**
	 * Create a Lookup object that will find records of the given name and type
	 * in the IN class.
	 * 
	 * @param name
	 *            The name of the desired records
	 * @param type
	 *            The type of the desired records
	 * @throws TextParseException
	 *             The name is not a valid DNS name
	 * @throws IllegalArgumentException
	 *             The type is a meta type other than ANY.
	 * @see #Lookup(Name,int,int)
	 */
	public AsynchronousLookup(String name, int type) throws TextParseException {
		this(Name.fromString(name), type, DClass.IN);
	}

	/**
	 * Create a Lookup object that will find records of type A at the given name
	 * in the IN class.
	 * 
	 * @param name
	 *            The name of the desired records
	 * @throws TextParseException
	 *             The name is not a valid DNS name
	 * @see #Lookup(Name,int,int)
	 */
	public AsynchronousLookup(String name) throws TextParseException {
		this(Name.fromString(name), Type.A, DClass.IN);
	}

	/**
	 * Sets the resolver to use when performing this lookup. This overrides the
	 * default value.
	 * 
	 * @param resolver
	 *            The resolver to use.
	 */
	public void setResolver(Resolver resolver) {
		this.resolver = resolver;
	}

	/**
	 * Sets the search path to use when performing this lookup. This overrides
	 * the default value.
	 * 
	 * @param domains
	 *            An array of names containing the search path.
	 */
	public void setSearchPath(Name[] domains) {
		this.searchPath = domains;
	}

	/**
	 * Sets the search path to use when performing this lookup. This overrides
	 * the default value.
	 * 
	 * @param domains
	 *            An array of names containing the search path.
	 * @throws TextParseException
	 *             A name in the array is not a valid DNS name.
	 */
	public void setSearchPath(String[] domains) throws TextParseException {
		if (domains == null) {
			this.searchPath = null;
			return;
		}
		Name[] newdomains = new Name[domains.length];
		for (int i = 0; i < domains.length; i++)
			newdomains[i] = Name.fromString(domains[i], Name.root);
		this.searchPath = newdomains;
	}

	/**
	 * Sets the cache to use when performing this lookup. This overrides the
	 * default value. If the results of this lookup should not be permanently
	 * cached, null can be provided here.
	 * 
	 * @param cache
	 *            The cache to use.
	 */
	public void setCache(Cache cache) {
		if (cache == null) {
			this.cache = new Cache(dclass);
			this.temporary_cache = true;
		} else {
			this.cache = cache;
			this.temporary_cache = false;
		}
	}

	/**
	 * Sets the minimum credibility level that will be accepted when performing
	 * the lookup. This defaults to Credibility.NORMAL.
	 * 
	 * @param credibility
	 *            The minimum credibility level.
	 */
	public void setCredibility(int credibility) {
		this.credibility = credibility;
	}

	private <A> void follow(Name name, Name oldname, A attachment, CompletionHandler<Record[],A> handler) {
		foundAlias = true;
		badresponse = false;
		networkerror = false;
		timedout = false;
		nxdomain = false;
		referral = false;
		iterations++;
		if (iterations >= 6 || name.equals(oldname)) {
			result = UNRECOVERABLE;
			error = "CNAME loop";
			done = true;
			handler.failed(new Exception(error),attachment);
			return;
		}
		if (aliases == null)
			aliases = new ArrayList<Name>();
		aliases.add(oldname);
		lookup(name, attachment, handler);
	}

	private <A> void processResponse(Name name, SetResponse response, A attachment, CompletionHandler<Record[],A> handler) {
		if (response.isSuccessful()) {
			RRset[] rrsets = response.answers();
			List<Record> l = new ArrayList<Record>();
			Iterator<Record> it;
			int i;

			for (i = 0; i < rrsets.length; i++) {
				it = rrsets[i].rrs();
				while (it.hasNext())
					l.add(it.next());
			}

			result = SUCCESSFUL;
			answers = l.toArray(new Record[l.size()]);
			done = true;
			handler.completed(getAnswers(),attachment);
		} else if (response.isNXDOMAIN()) {
			nxdomain = true;
			doneCurrent = true;
			//if (iterations > 0) {
				result = HOST_NOT_FOUND;
				done = true;
				handler.failed(new UnknownHostException(),attachment);
		//	}
		} else if (response.isNXRRSET()) {
			result = TYPE_NOT_FOUND;
			answers = null;
			done = true;
			handler.failed(new Exception(getErrorString()),attachment);
		} else if (response.isCNAME()) {
			CNAMERecord cname = response.getCNAME();
			follow(cname.getTarget(), name, attachment, handler);
		} else if (response.isDNAME()) {
			DNAMERecord dname = response.getDNAME();
//			Name newname = null;
			try {
				follow(name.fromDNAME(dname), name, attachment, handler);
			} catch (NameTooLongException e) {
				result = UNRECOVERABLE;
				error = "Invalid DNAME target";
				done = true;
//				checkError();
				handler.failed(new Exception(error),attachment);
			}
		} else if (response.isDelegation()) {
			// We shouldn't get a referral. Ignore it.
			referral = true;
			System.err.println("AsynchronousLookup got a REFERRAL");
		}
	}

	private synchronized <A> void lookup(final Name current, final A attachment, final CompletionHandler<Record[],A> handler) {
//		SetResponse sr = cache.lookupRecords(current, type, credibility);
//		if (verbose) {
//			System.err.println("lookup " + current + " " + Type.string(type));
//			System.err.println(sr);
//		}
//		processResponse(current, sr, attachment, handler);
//		if (done || doneCurrent)
//			return;

		final Record question = Record.newRecord(current, type, dclass);
		final Message query = Message.newQuery(question);
//		Message response = null;
		resolver.sendAsync(query, new ResolverListener() {

			public void handleException(Object id, Exception e) {
				if (e instanceof InterruptedIOException || e instanceof TimeoutException)
					timedout = true;
				else
					networkerror = true;
				checkError();
				handler.failed(e,attachment);
			}

			public void receiveMessage(Object id, Message response) {
				int rcode = response.getHeader().getRcode();
				
				if (rcode != Rcode.NOERROR && rcode != Rcode.NXDOMAIN) {
					if(rcode == Rcode.SERVFAIL)
						result = TRY_AGAIN;
					// The server we contacted is broken or otherwise unhelpful.
					// Press on.
					badresponse = true;
					badresponse_error = Rcode.string(rcode);
					checkError();
					handler.failed(new Exception(getErrorString()),attachment);
					return;
				}

				if (!query.getQuestion().equals(response.getQuestion())) {
					// The answer doesn't match the question. That's not good.
					badresponse = true;
					badresponse_error = "response does not match query";
					checkError();
					handler.failed(new Exception(getErrorString()),attachment);
					return;
				}

				SetResponse sr = cache.addMessage(response);
				if (sr == null)
					sr = cache.lookupRecords(current, type, credibility);
				if (verbose) {
					System.err.println("queried " + current + " "
							+ Type.string(type));
					System.err.println(sr);
				}
				processResponse(current, sr, attachment, handler);
			}

		});
	}

	private synchronized <A> void resolve(Name current, Name suffix, A attachment, CompletionHandler<Record[],A> handler) {
		doneCurrent = false;
		Name tname = null;
		if (suffix == null)
			tname = current;
		else {
			try {
				tname = Name.concatenate(current, suffix);
			} catch (NameTooLongException e) {
				nametoolong = true;
				checkError();
				handler.failed(e, attachment);
				return;
			}
		}
		lookup(tname, attachment, handler);
	}

	/**
	 * Performs the lookup, using the specified Cache, Resolver, and search
	 * path.
	 * 
	 * @return The answers, or null if none are found.
	 */
	public synchronized <A> void run(A attachment, CompletionHandler<Record[],A> handler) {
		if (done)
			reset();
		if (name.isAbsolute())
			resolve(name, null, attachment, handler);
		else if (searchPath == null)
			resolve(name, Name.root, attachment, handler);
		else {
			System.err.println("AsynchronousLookup error: search path not implemented");
			throw new RuntimeException(this.getClass().getName()+": Search path not implemented");
/*			if (name.labels() > 1)
				resolve(name, Name.root, handler);
			if (done)
				return;

			for (int i = 0; i < searchPath.length; i++) {
				resolve(name, searchPath[i], handler);
				if (done)
					return answers;
				else if (foundAlias)
					break;
			}
	*/	}
	}
		
	private void checkError() {
		if (!done) {
			if (badresponse) {
				result = TRY_AGAIN;
				error = badresponse_error;
				done = true;
			} else if (timedout) {
				result = TRY_AGAIN;
				error = "timed out";
				done = true;
			} else if (networkerror) {
				result = TRY_AGAIN;
				error = "network error";
				done = true;
			} else if (nxdomain) {
				result = HOST_NOT_FOUND;
				done = true;
			} else if (referral) {
				result = UNRECOVERABLE;
				error = "referral";
				done = true;
			} else if (nametoolong) {
				result = UNRECOVERABLE;
				error = "name too long";
				done = true;
			}
			if (verbose)
				System.err.println(name+" lookup failed: "+getErrorString());
		}
	}

	private void checkDone() {
		if (done && result != -1)
			return;
		StringBuffer sb = new StringBuffer("Lookup of " + name + " ");
		if (dclass != DClass.IN)
			sb.append(DClass.string(dclass) + " ");
		sb.append(Type.string(type) + " isn't done");
		System.err.println("AsynchronousLookup in illegal state: "+sb.toString());
		throw new IllegalStateException(sb.toString());
	}

	/**
	 * Returns the answers from the lookup.
	 * 
	 * @return The answers, or null if none are found.
	 * @throws IllegalStateException
	 *             The lookup has not completed.
	 */
	public Record[] getAnswers() {
		checkDone();
		return answers;
	}

	/**
	 * Returns all known aliases for this name. Whenever a CNAME/DNAME is
	 * followed, an alias is added to this array. The last element in this array
	 * will be the owner name for records in the answer, if there are any.
	 * 
	 * @return The aliases.
	 * @throws IllegalStateException
	 *             The lookup has not completed.
	 */
	public Name[] getAliases() {
		checkDone();
		if (aliases == null)
			return noAliases;
		return aliases.toArray(new Name[aliases.size()]);
	}

	/**
	 * Returns the result code of the lookup.
	 * 
	 * @return The result code, which can be SUCCESSFUL, UNRECOVERABLE,
	 *         TRY_AGAIN, HOST_NOT_FOUND, or TYPE_NOT_FOUND.
	 * @throws IllegalStateException
	 *             The lookup has not completed.
	 */
	public int getResult() {
		checkDone();
		return result;
	}

	/**
	 * Returns an error string describing the result code of this lookup.
	 * 
	 * @return A string, which may either directly correspond the result code or
	 *         be more specific.
	 * @throws IllegalStateException
	 *             The lookup has not completed.
	 */
	public String getErrorString() {
		checkDone();
		if (error != null)
			return error;
		switch (result) {
		case SUCCESSFUL:
			return "successful";
		case UNRECOVERABLE:
			return "unrecoverable error";
		case TRY_AGAIN:
			return "try again";
		case HOST_NOT_FOUND:
			return "host not found";
		case TYPE_NOT_FOUND:
			return "type not found";
		}
		System.err.println("AsynchronousLookup getErrorString() cannot determine error string for result="+result);
		throw new IllegalStateException("unknown result");
	}

}
