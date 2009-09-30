package com.netifera.platform.api.iterables;

import java.io.Serializable;


public interface IndexedIterable<T> extends FiniteIterable<T>, Serializable {
	T itemAt(int index);
}
