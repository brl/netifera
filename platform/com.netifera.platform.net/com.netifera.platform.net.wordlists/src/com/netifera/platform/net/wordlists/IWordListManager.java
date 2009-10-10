package com.netifera.platform.net.wordlists;

import java.util.List;

public interface IWordListManager {
	IWordList getWordListByName(String name);
	List<IWordList> getWordListsByCategories(String[] categories);
}
