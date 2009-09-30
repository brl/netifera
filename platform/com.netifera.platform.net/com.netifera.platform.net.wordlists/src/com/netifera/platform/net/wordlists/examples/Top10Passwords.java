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

	public String getCategory() {
		return IWordList.CATEGORY_PASSWORDS;
	}

	public FiniteIterable<String> getWords() {
		List<String> words = new ArrayList<String>();
//		for (String word: "123 password liverpool letmein 123456 qwerty charlie monkey arsenal thomas".split(" "))
		for (String word: "123 password 123456 qwerty letmein monkey password1 abc123 12345678 1234".split(" "))
			words.add(word);
		return new ListIndexedIterable<String>(words);
	}
}
