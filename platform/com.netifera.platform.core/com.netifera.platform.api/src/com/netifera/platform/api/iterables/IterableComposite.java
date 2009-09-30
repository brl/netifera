package com.netifera.platform.api.iterables;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class IterableComposite<T> implements FiniteIterable<T> {

	private List<Iterable<T>> iterables = new ArrayList<Iterable<T>>();

	public void add(FiniteIterable<T> iterable) {
		iterables.add(iterable);
	}

	public void add(Collection<T> collection) {
		iterables.add(collection);
	}

	public int itemCount() {
		int count = 0;
		for (Iterable<T> iterable: iterables)
			if (iterable instanceof Collection)
				count += ((Collection<T>)iterable).size();
			else //if (iterable instanceof FiniteIterable)
				count += ((FiniteIterable<T>)iterable).itemCount();
		return count;
	}

	public Iterator<T> iterator() {
		List<Iterator<T>> iterators = new ArrayList<Iterator<T>>();
		for (Iterable<T> iterable: iterables)
			iterators.add(iterable.iterator());
		return new IteratorComposite<T>(iterators);
	}
}
