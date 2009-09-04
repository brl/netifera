package com.netifera.platform.net.wordlists;

public interface IWordMangler {
	String getName();
	String[] mangle(String word);
	int size();
}
