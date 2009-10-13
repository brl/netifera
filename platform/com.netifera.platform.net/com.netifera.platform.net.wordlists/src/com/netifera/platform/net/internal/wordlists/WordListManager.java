package com.netifera.platform.net.internal.wordlists;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.osgi.service.component.ComponentContext;

import com.netifera.platform.api.log.ILogManager;
import com.netifera.platform.api.log.ILogger;
import com.netifera.platform.net.wordlists.IWordList;
import com.netifera.platform.net.wordlists.IWordListManager;

public class WordListManager implements IWordListManager {

	private ILogger logger;
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

	protected void activate(ComponentContext context) {
		String basePath = System.getProperty("user.home", System.getenv("HOME")) + File.separator + ".netifera" + File.separator + "data" + File.separator + "wordlists" + File.separator;

		File wordlistsDirectory = new File(basePath);
		if (!wordlistsDirectory.exists())
			return;
		for (File category: wordlistsDirectory.listFiles()) {
			if (category.isDirectory())
				for (File file: category.listFiles()) {
					if (!file.isDirectory())
						registerWordList(new FileWordList(file.getName(), category.getName(), file));
				}
			else
				registerWordList(new FileWordList(category.getName(), IWordList.CATEGORY_PASSWORDS, category));
		}
	}

	protected void deactivate(ComponentContext context) {
	}
	
	protected void setLogManager(ILogManager logManager) {
		this.logger = logManager.getLogger("Wordlists Service");
	}
	
	protected void unsetLogManager(ILogManager logManager) {
	}

	protected void registerWordList(IWordList wordlist) {
		this.wordlists.add(wordlist);
		if (logger != null)
			logger.info("Registered wordlist: "+wordlist.getName());
	}
	
	protected void unregisterWordList(IWordList wordlist) {
		this.wordlists.remove(wordlist);
	}
}
