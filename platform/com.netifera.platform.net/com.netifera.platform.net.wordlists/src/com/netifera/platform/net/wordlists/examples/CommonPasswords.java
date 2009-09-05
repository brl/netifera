package com.netifera.platform.net.wordlists.examples;

import java.util.ArrayList;
import java.util.List;

import com.netifera.platform.api.iterables.FiniteIterable;
import com.netifera.platform.api.iterables.ListIndexedIterable;
import com.netifera.platform.net.wordlists.IWordList;

public class CommonPasswords implements IWordList {

	public String getName() {
		return "Common Passwords (including username variations)";
	}

	public FiniteIterable<String> getWords() {
		List<String> words = new ArrayList<String>();
		for (String word: "%username% %username%0 %username%01 %username%12 %username%123 %username%1234 %username%12345 123 password 123456 qwerty letmein monkey password1 abc123 12345678 1234 12345 passwd pass".split(" "))
			words.add(word);
		return new ListIndexedIterable<String>(words);
	}
}
