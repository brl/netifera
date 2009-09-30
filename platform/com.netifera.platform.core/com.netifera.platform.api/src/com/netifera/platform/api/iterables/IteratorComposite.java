package com.netifera.platform.api.iterables;

import java.util.Collection;
import java.util.Iterator;

public class IteratorComposite<T> implements Iterator<T> {

	final private Iterator<Iterator<T>> iterators;
	private Iterator<T> currentIterator;
	
	public IteratorComposite(Collection<Iterator<T>> iteratorsCollection) {
		iterators = iteratorsCollection.iterator();
		// XXX assumes not empty iteratorsCollection
		currentIterator = iterators.next();
	}

	public boolean hasNext() {
		// XXX assumes no empty iterators
		return currentIterator.hasNext() || iterators.hasNext();
	}

	public T next() {
		if (!currentIterator.hasNext())
			currentIterator = iterators.next();
		return currentIterator.next();
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}
}
