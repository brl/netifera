package com.netifera.platform.net.wordlists.examples;

import java.util.ArrayList;
import java.util.List;

import com.netifera.platform.api.iterables.FiniteIterable;
import com.netifera.platform.api.iterables.ListIndexedIterable;
import com.netifera.platform.net.wordlists.IWordList;

public class CommonAPPasswords implements IWordList {
	
	public String getName() {
		return "Common AP Passwords";
	}
	
	public String getCategory() {
		return IWordList.CATEGORY_PASSWORDS;
	}

	public FiniteIterable<String> getWords() {
		List<String> words = new ArrayList<String>();
		for (String word: "admin root 1234 password administrator manager 123456 123 qwerasdf qwerty".split(" "))
			words.add(word);
		return new ListIndexedIterable<String>(words);
	}
}
