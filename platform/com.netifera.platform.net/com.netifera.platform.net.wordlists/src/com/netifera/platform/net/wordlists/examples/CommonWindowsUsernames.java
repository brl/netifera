package com.netifera.platform.net.wordlists.examples;

import java.util.ArrayList;
import java.util.List;

import com.netifera.platform.api.iterables.FiniteIterable;
import com.netifera.platform.api.iterables.ListIndexedIterable;
import com.netifera.platform.net.wordlists.IWordList;

public class CommonWindowsUsernames implements IWordList {

	public String getName() {
		return "Common Windows Usernames";
	}

	public String getCategory() {
		return IWordList.CATEGORY_USERNAMES;
	}

	public FiniteIterable<String> getWords() {
		List<String> words = new ArrayList<String>();
		for (String word: "Administrator Guest TsInternetUser backup test user".split(" "))
			words.add(word);
		return new ListIndexedIterable<String>(words);
	}
}
