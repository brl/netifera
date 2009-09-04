package com.netifera.platform.api.iterables;

import java.io.Serializable;

public interface FiniteIterable<T> extends Iterable<T>, Serializable {
	int itemCount();
}
