package com.netifera.platform.net.wordlists.examples;

import java.util.ArrayList;
import java.util.List;

import com.netifera.platform.api.iterables.FiniteIterable;
import com.netifera.platform.api.iterables.ListIndexedIterable;
import com.netifera.platform.net.wordlists.IWordList;

public class CommonFemaleNames implements IWordList {

	public String getName() {
		return "Female Names in USA (27)";
	}

	public String getCategory() {
		return IWordList.CATEGORY_NAMES;
	}

	public FiniteIterable<String> getWords() {
		List<String> words = new ArrayList<String>();
		for (String word: "jennifer lisa kimberly michelle amy angela melissa tammy mary tracy julie sephanie heather nicole rebecca jessica amanda kelly sarah elizabeth crystal ashley megan brittany samantha lauren".split(" "))
			words.add(word);
		return new ListIndexedIterable<String>(words);
	}

}
