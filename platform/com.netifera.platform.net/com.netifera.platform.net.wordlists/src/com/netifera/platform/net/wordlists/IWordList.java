package com.netifera.platform.net.wordlists;

import com.netifera.platform.api.iterables.FiniteIterable;


public interface IWordList {
	String getName();
	FiniteIterable<String> getWords();
}
