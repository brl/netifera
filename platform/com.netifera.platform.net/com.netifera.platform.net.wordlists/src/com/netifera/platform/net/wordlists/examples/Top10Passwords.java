package com.netifera.platform.net.wordlists.examples;

import java.util.ArrayList;
import java.util.List;

import com.netifera.platform.api.iterables.FiniteIterable;
import com.netifera.platform.api.iterables.ListIndexedIterable;
import com.netifera.platform.net.wordlists.IWordList;

public class Top10Passwords implements IWordList {

	public String getName() {
		return "Top 10 Passwords";
	}

	public FiniteIterable<String> getWords() {
		List<String> words = new ArrayList<String>();
		for (String word: "123 password liverpool letmein 123456 qwerty charlie monkey arsenal thomas".split(" "))
			words.add(word);
		return new ListIndexedIterable<String>(words);
	}
}
