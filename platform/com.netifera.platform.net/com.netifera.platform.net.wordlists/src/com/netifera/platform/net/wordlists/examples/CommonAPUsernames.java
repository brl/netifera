package com.netifera.platform.net.wordlists.examples;

import java.util.ArrayList;
import java.util.List;

import com.netifera.platform.api.iterables.FiniteIterable;
import com.netifera.platform.api.iterables.ListIndexedIterable;
import com.netifera.platform.net.wordlists.IWordList;

public class CommonAPUsernames implements IWordList {


	public String getName() {
		return "Common AP Usernames";
	}

	public FiniteIterable<String> getWords() {
		List<String> words = new ArrayList<String>();
		for (String word: "root admin administrator manager".split(" "))
			words.add(word);
		return new ListIndexedIterable<String>(words);
	}

}
