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

	public FiniteIterable<String> getWords() {
		List<String> words = new ArrayList<String>();
		for (String word: "root ftp guest test admin web webmaster www mysql oracle postgres apache tomcat backup postmaster mail video audio remote shop sales".split(" "))
			words.add(word);
		return new ListIndexedIterable<String>(words);
	}

}
