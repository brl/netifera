package com.netifera.platform.net.wordlists.examples;

import java.util.ArrayList;
import java.util.List;

import com.netifera.platform.api.iterables.FiniteIterable;
import com.netifera.platform.api.iterables.ListIndexedIterable;
import com.netifera.platform.net.wordlists.IWordList;

public class KeyboardPatterns implements IWordList {

	public String getName() {
		return "Keyboard Patterns (28)";
	}

	public String getCategory() {
		return IWordList.CATEGORY_PASSWORDS;
	}

	public FiniteIterable<String> getWords() {
		List<String> words = new ArrayList<String>();
		for (String word: "123 123456 qwerty abc123 12345678 1234 asdf 159357 123456789 1234567 111111 654321 777777 666666 555555 1qaz2wsx 1q2w3e 1234qwer zaq1xsw2 zxcvbnm abcd1234 a1b2c3d4 zxcvasdf zaxscd qeadzc !@#$ !@#$%^&* !Q@W#E".split(" "))
			words.add(word);
		return new ListIndexedIterable<String>(words);
	}
}
