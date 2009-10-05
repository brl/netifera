// Copyright (c) 1999-2004 Brian Wellington (bwelling@xbill.org)

package com.netifera.platform.net.dns.service.client;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.xbill.DNS.ARecord;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Options;
import org.xbill.DNS.PTRRecord;
import org.xbill.DNS.Record;
import org.xbill.DNS.Resolver;
import org.xbill.DNS.ResolverListener;
import org.xbill.DNS.ReverseMap;
import org.xbill.DNS.TSIG;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

import com.netifera.platform.util.addresses.inet.InternetAddress;

/**
 * An implementation of Resolver that can send queries to multiple servers,
 * sending the queries multiple times if necessary.
 * 
 * @see Resolver
 * 
 * @author Brian Wellington
 * @author lee
 */

public class ExtendedResolver implements Resolver {

	private static class Resolution implements ResolverListener {
		Resolver[] resolvers;
		int[] sent;
		Object[] inprogress;
		int retries;
		int outstanding;
		boolean done;
		Message query;
		Message response;
		Throwable thrown;
		ResolverListener listener;

		public Resolution(ExtendedResolver eres, Message query) {
			assert eres.resolvers.size() != 0;
			/*
			 * Note: an empty eres.resolvers will generates a
			 * ArrayIndexOutOfBoundsException in all the Resolution methods
			 * 
			 * 			sent = new int[resolvers.length];
			 *          inprogress = new Object[resolvers.length];
			 *          
			 *          sent[0]++; // ArrayIndexOutOfBoundsException
			 */
			List<Resolver> l = eres.resolvers;
			resolvers = l.toArray(new Resolver[l.size()]);
			if (eres.loadBalance) {
				int nresolvers = resolvers.length;
				/*
				 * Note: this is not synchronized, since the worst thing that
				 * can happen is a random ordering, which is ok.
				 */
				int start = eres.lbStart++ % nresolvers;
				if (eres.lbStart > nresolvers)
					eres.lbStart %= nresolvers;
				if (start > 0) {
					Resolver[] shuffle = new Resolver[nresolvers];
					for (int i = 0; i < nresolvers; i++) {
						int pos = (i + start) % nresolvers;
						shuffle[i] = resolvers[pos];
					}
					resolvers = shuffle;
				}
			}
			sent = new int[resolvers.length];
			inprogress = new Object[resolvers.length];
			retries = eres.retries;
			this.query = query;
		}

		/* Asynchronously sends a message. */
		public void send(int n) {
			sent[n]++;
			outstanding++;
			try {
				inprogress[n] = resolvers[n].sendAsync(query, this);
			} catch (Throwable t) {
				synchronized (this) {
					thrown = t;
					done = true;
					if (listener == null) {
						notifyAll();
						return;
					}
				}
			}
		}

		/* Start a synchronous resolution */
		public Message start() throws IOException {
			try {
				/*
				 * First, try sending synchronously. If this works, we're done.
				 * Otherwise, we'll get an exception and continue. It would be
				 * easier to call send(0), but this avoids a thread creation. If
				 * and when SimpleResolver.sendAsync() can be made to not create
				 * a thread, this could be changed.
				 */
				sent[0]++;
				outstanding++;
				inprogress[0] = new Object();
				return resolvers[0].send(query);
			} catch (Exception e) {
				/*
				 * This will either cause more queries to be sent asynchronously
				 * or will set the 'done' flag.
				 */
				handleException(inprogress[0], e);
			}
			/*
			 * Wait for a successful response or for each subresolver to
			 * fail.
			 */
			synchronized (this) {
				while (!done) {
					try {
						wait();
					} catch (InterruptedException e) {
						//HACK is this fine? used to not do anything, and never timed out and couldnt cancell tasks. len
//						done = true;
						InterruptedIOException e2 = new InterruptedIOException("Interrupted");
						e2.initCause(e);
						throw e2;
					}
				}
			}
			/* Return the response or throw an exception */
			if (response != null)
				return response;
			else if (thrown instanceof IOException)
				throw (IOException) thrown;
			else if (thrown instanceof RuntimeException)
				throw (RuntimeException) thrown;
			else if (thrown instanceof Error)
				throw (Error) thrown;
			else
				throw new IllegalStateException("ExtendedResolver failure");
		}

		/* Start an asynchronous resolution */
		public void startAsync(ResolverListener listener) {
			this.listener = listener;
			send(0);
		}

		/*
		 * Receive a response. If the resolution hasn't been completed, either
		 * wake up the blocking thread or call the callback.
		 */
		public void receiveMessage(Object id, Message m) {
			if (Options.check("verbose"))
				System.err.println("ExtendedResolver: " + "received message");
			synchronized (this) {
				if (done)
					return;
				response = m;
				done = true;
				if (listener == null) {
					notifyAll();
					return;
				}
			}
			listener.receiveMessage(this, response);
		}

		/*
		 * Receive an exception. If the resolution has been completed, do
		 * nothing. Otherwise make progress.
		 */
		public void handleException(Object id, Exception e) {
			if (Options.check("verbose"))
				System.err.println("ExtendedResolver: got " + e);
			synchronized (this) {
				outstanding--;
				if (done)
					return;
				int n;
				for (n = 0; n < inprogress.length; n++)
					if (inprogress[n] == id)
						break;
				/* If we don't know what this is, do nothing. */
				if (n == inprogress.length)
					return;
				boolean startnext = false;
				/*
				 * If this is the first response from server n, we should start
				 * sending queries to server n + 1.
				 */
				if (sent[n] == 1 && n < resolvers.length - 1)
					startnext = true;
				if (e instanceof InterruptedIOException) {
					/* Got a timeout; resend */
					if (sent[n] < retries)
						send(n);
					if (thrown == null)
						thrown = e;
				} else if (e instanceof SocketException) {
					/*
					 * Problem with the socket; don't resend on it
					 */
					if (thrown == null
							|| thrown instanceof InterruptedIOException)
						thrown = e;
				} else {
					/*
					 * Problem with the response; don't resend on the same
					 * socket.
					 */
					thrown = e;
				}
				if (done)
					return;
				if (startnext)
					send(n + 1);
				if (done)
					return;
				if (outstanding == 0) {
					/*
					 * If we're done and this is synchronous, wake up the
					 * blocking thread.
					 */
					done = true;
					if (listener == null) {
						notifyAll();
						return;
					}
				}
				if (!done)
					return;
			}
			/* If we're done and this is asynchronous, call the callback. */
			if (!(thrown instanceof Exception))
				thrown = new RuntimeException(thrown.getMessage());
			listener.handleException(this, (Exception) thrown);
		}
	}

	private List<Resolver> resolvers = new ArrayList<Resolver>();;
	private boolean loadBalance = false;
	private int lbStart = 0;
	private int retries = 3;

	/**
	 * Creates a new Extended Resolver. The default ResolverConfig is used to
	 * determine the servers for which SimpleResolver contexts should be
	 * initialized.
	 * 
	 * @see SimpleResolver
	 * @see ResolverConfig
	 * @exception UnknownHostException
	 *                Failure occured initializing SimpleResolvers
	 */
	/*
	 * public ExtendedResolver() throws UnknownHostException { init(); // XXX
	 * ResolverConfig is deprecated String [] servers =
	 * ResolverConfig.getCurrentConfig().servers(); if (servers != null) { for
	 * (int i = 0; i < servers.length; i++) { Resolver r = new
	 * SimpleResolver(servers[i]); r.setTimeout(quantum); resolvers.add(r); } }
	 * else resolvers.add(new SimpleResolver()); }
	 */

	/**
	 * Creates a new Extended Resolver
	 * 
	 * @param servers
	 *            An array of server names for which SimpleResolver contexts
	 *            should be initialized.
	 * @see SimpleResolver
	 * @exception UnknownHostException
	 *                Failure occured initializing SimpleResolvers
	 */
	/*
	 * public ExtendedResolver(UDPSocketLocator[] servers) throws
	 * UnknownHostException { init(); for (int i = 0; i < servers.length; i++) {
	 * Resolver r = new SimpleResolver(servers[i]); r.setTimeout(quantum);
	 * resolvers.add(r); } }
	 */

	public ExtendedResolver() {
	}
	
	/**
	 * Creates a new Extended Resolver
	 * 
	 * @param res
	 *            An array of pre-initialized Resolvers is provided.
	 * @see SimpleResolver
	 * @exception UnknownHostException
	 *                Failure occured initializing SimpleResolvers
	 */
/*	public ExtendedResolver(Resolver[] res) {
		for (int i = 0; i < res.length; i++)
			resolvers.add(res[i]);
	}
*/
	@Deprecated
	public void setPort(int port) {
		for (int i = 0; i < resolvers.size(); i++)
			resolvers.get(i).setPort(port);
	}

	@Deprecated
	public void setTCP(boolean flag) {
		for (int i = 0; i < resolvers.size(); i++)
			resolvers.get(i).setTCP(flag);
	}

	public void setIgnoreTruncation(boolean flag) {
		for (int i = 0; i < resolvers.size(); i++)
			resolvers.get(i).setIgnoreTruncation(flag);
	}

	public void setEDNS(int level) {
		for (int i = 0; i < resolvers.size(); i++)
			resolvers.get(i).setEDNS(level);
	}

	@SuppressWarnings("unchecked")
	public void setEDNS(int level, int payloadSize, int flags, List options) {
		for (int i = 0; i < resolvers.size(); i++)
			resolvers.get(i).setEDNS(level, payloadSize, flags, options);
	}

	public void setTSIGKey(TSIG key) {
		for (int i = 0; i < resolvers.size(); i++)
			resolvers.get(i).setTSIGKey(key);
	}

	public void setTimeout(int secs, int msecs) {
		for (int i = 0; i < resolvers.size(); i++)
			resolvers.get(i).setTimeout(secs, msecs);
	}

	public void setTimeout(int secs) {
		setTimeout(secs, 0);
	}

	/**
	 * Sends a message and waits for a response. Multiple servers are queried,
	 * and queries are sent multiple times until either a successful response is
	 * received, or it is clear that there is no successful response.
	 * 
	 * @param query
	 *            The query to send.
	 * @return The response.
	 * @throws IOException
	 *             An error occurred while sending or receiving.
	 */
	public Message send(Message query) throws IOException {
		Resolution res = new Resolution(this, query);
		return res.start();
	}

	/**
	 * Asynchronously sends a message to multiple servers, potentially multiple
	 * times, registering a listener to receive a callback on success or
	 * exception. Multiple asynchronous lookups can be performed in parallel.
	 * Since the callback may be invoked before the function returns, external
	 * synchronization is necessary.
	 * 
	 * @param query
	 *            The query to send
	 * @param listener
	 *            The object containing the callbacks.
	 * @return An identifier, which is also a parameter in the callback
	 */
	public Object sendAsync(final Message query, final ResolverListener listener) {
		Resolution res = new Resolution(this, query);
		res.startAsync(listener);
		return res;
	}

	/** Returns the nth resolver used by this ExtendedResolver */
	public Resolver getResolver(int n) {
		if (n < resolvers.size())
			return resolvers.get(n);
		return null;
	}

	/** Returns all resolvers used by this ExtendedResolver */
	public Resolver[] getResolvers() {
		return resolvers.toArray(new Resolver[resolvers.size()]);
	}

	/** Adds a new resolver to be used by this ExtendedResolver */
	public void addResolver(Resolver r) {
		resolvers.add(r);
	}

	/** Deletes a resolver used by this ExtendedResolver */
	public void deleteResolver(Resolver r) {
		resolvers.remove(r);
	}

	/**
	 * Sets whether the servers should be load balanced.
	 * 
	 * @param flag
	 *            If true, servers will be tried in round-robin order. If false,
	 *            servers will always be queried in the same order.
	 */
	public void setLoadBalance(boolean flag) {
		loadBalance = flag;
	}

	/** Sets the number of retries sent to each server per query */
	public void setRetries(int retries) {
		this.retries = retries;
	}
	
	public InternetAddress getAddressByName(String name) throws TextParseException, UnknownHostException {
		Lookup lookup = new Lookup(name);
		lookup.setResolver(this);
		lookup.setSearchPath((Name[])null);

		for (Record record: lookup.run())
			if (record instanceof ARecord)
				return InternetAddress.fromInetAddress(((ARecord)record).getAddress());
		throw new UnknownHostException(name);
	}

	public List<InternetAddress> getAddressesByName(String name) throws UnknownHostException {
		Lookup lookup;
		try {
			lookup = new Lookup(name);
		} catch (TextParseException e) {
			throw new UnknownHostException("Malformed host name: "+ name);
		}
		lookup.setResolver(this);
		lookup.setSearchPath((Name[])null);

		List<InternetAddress> answer = new ArrayList<InternetAddress>();
		for (Record record: lookup.run())
			if (record instanceof ARecord)
				answer.add(InternetAddress.fromInetAddress(((ARecord)record).getAddress()));
		return answer;
	}

	public String getNameByAddress(InternetAddress address) throws UnknownHostException {
		Name name = ReverseMap.fromAddress(address.toString());
		Lookup lookup = new Lookup(name, Type.PTR, DClass.IN);
		lookup.setResolver(this);
		lookup.setSearchPath((Name[])null);

		for (Record record: lookup.run())
			if (record instanceof PTRRecord)
				return ((PTRRecord)record).getName().toString();
		throw new UnknownHostException(address.toString());
	}

	public List<String> getNamesByAddress(InternetAddress address) throws UnknownHostException {
		Name name = ReverseMap.fromAddress(address.toString());
		Lookup lookup = new Lookup(name, Type.PTR, DClass.IN);
		lookup.setResolver(this);
		lookup.setSearchPath((Name[])null);

		List<String> answer = new ArrayList<String>();
		for (Record record: lookup.run())
			if (record instanceof PTRRecord)
				answer.add(((PTRRecord)record).getName().toString());
		return answer;
	}
	
	public synchronized void shutdown() throws IOException {
		for (Resolver resolver: resolvers) {
			if (resolver instanceof SimpleResolver) {
				((SimpleResolver)resolver).shutdown();
			} else if (resolver instanceof ExtendedResolver) {
				((ExtendedResolver)resolver).shutdown();
			}
		}
	}
}
