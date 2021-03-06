package com.netifera.platform.net.wordlists.examples;

import java.util.ArrayList;
import java.util.List;

import com.netifera.platform.api.iterables.FiniteIterable;
import com.netifera.platform.api.iterables.ListIndexedIterable;
import com.netifera.platform.net.wordlists.IWordList;

public class CommonUnixUsernames implements IWordList {

	public String getName() {
		return "Common Unix Usernames";
	}

	public String getCategory() {
		return IWordList.CATEGORY_USERNAMES;
	}

	public FiniteIterable<String> getWords() {
		List<String> words = new ArrayList<String>();
		for (String word: "root admin test guest user ftp web webmaster www mysql oracle postgres db apache tomcat backup postmaster mail video audio remote support info help shop sales marketing".split(" "))
			words.add(word);
		return new ListIndexedIterable<String>(words);
	}

}
