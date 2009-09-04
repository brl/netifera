package com.netifera.platform.net.wordlists.examples;

import java.util.ArrayList;
import java.util.List;

import com.netifera.platform.api.iterables.FiniteIterable;
import com.netifera.platform.api.iterables.ListIndexedIterable;
import com.netifera.platform.net.wordlists.IWordList;

public class CommonMaleNames implements IWordList {

	public String getName() {
		return "Common Male Names in USA";
	}

	public FiniteIterable<String> getWords() {
		List<String> words = new ArrayList<String>();
		for (String word: "david michael john james robert mark william richard thomas steven kenneth jeffrey kevin joseph christopher brian jason scott matthew daniel joshua andrew justin".split(" "))
			words.add(word);
		return new ListIndexedIterable<String>(words);
	}
}
