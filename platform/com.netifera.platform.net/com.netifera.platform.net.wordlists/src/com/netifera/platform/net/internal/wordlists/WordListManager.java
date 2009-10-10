package com.netifera.platform.net.internal.wordlists;

import java.util.ArrayList;
import java.util.List;

import com.netifera.platform.net.wordlists.IWordList;
import com.netifera.platform.net.wordlists.IWordListManager;

public class WordListManager implements IWordListManager {

	private List<IWordList> wordlists = new ArrayList<IWordList>();

	public IWordList getWordListByName(String name) {
		for (IWordList wordlist: wordlists) {
			if (wordlist.getName().equals(name))
				return wordlist;
		}
		return null;
	}
	
	public List<IWordList> getWordListsByCategories(String[] categories) {
		List<IWordList> answer = new ArrayList<IWordList>();
		for (IWordList wordlist: wordlists) {
			for (String category: categories) {
				if (wordlist.getCategory().equals(category)) {
					answer.add(wordlist);
					break;
				}
			}
		}
		return answer;
	}

	
	protected void registerWordList(IWordList wordlist) {
		this.wordlists.add(wordlist);
	}
	
	protected void unregisterWordList(IWordList wordlist) {
		this.wordlists.remove(wordlist);
	}
}
