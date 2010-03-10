package com.netifera.platform.net.wordlists;

import com.netifera.platform.api.iterables.FiniteIterable;


public interface IWordList {
	public static String CATEGORY_PASSWORDS = "passwords";
	public static String CATEGORY_USERNAMES = "usernames";
	public static String CATEGORY_NAMES = "names";
	
	String getName();
	String getCategory();
	FiniteIterable<String> getWords();
}
