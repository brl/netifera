package com.netifera.platform.net.wordlists.examples;

import java.util.ArrayList;
import java.util.List;

import com.netifera.platform.api.iterables.FiniteIterable;
import com.netifera.platform.api.iterables.ListIndexedIterable;
import com.netifera.platform.net.wordlists.IWordList;

public class CommonMaleNames implements IWordList {

	public String getName() {
		return "Male Names in USA";
	}

	public String getCategory() {
		return IWordList.CATEGORY_NAMES;
	}

	public FiniteIterable<String> getWords() {
		List<String> words = new ArrayList<String>();
		for (String word: "david michael john james robert mark william richard rick thomas tom steven kenneth jeffrey jeff kevin joseph christopher chris brian jason scott matthew matt daniel danny dan joshua andrew andy justin".split(" "))
			words.add(word);
		return new ListIndexedIterable<String>(words);
	}
}
