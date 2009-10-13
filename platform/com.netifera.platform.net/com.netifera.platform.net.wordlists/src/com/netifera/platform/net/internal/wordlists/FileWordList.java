package com.netifera.platform.net.internal.wordlists;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;

import com.netifera.platform.api.iterables.FiniteIterable;
import com.netifera.platform.net.wordlists.IWordList;

public class FileWordList implements IWordList {

	final private String name, category;
	final private File file;
	
	public FileWordList(String name, String category, File file) {
		this.name = name;
		this.category = category;
		this.file = file;
	}
	
	public String getCategory() {
		return category;
	}

	public String getName() {
		return name;
	}

	public FiniteIterable<String> getWords() {
		try {
			final BufferedReader reader = new BufferedReader(new FileReader(file));
			return new FiniteIterable<String>() {
				Integer itemCount = null;
				
				public int itemCount() {
					if (itemCount != null)
						return itemCount;
					itemCount = 0;
					try {
						BufferedReader reader = new BufferedReader(new FileReader(file));
						while (reader.readLine() != null)
							itemCount += 1;
					} catch (IOException e) {
					}
					return itemCount;
				}
	
				public Iterator<String> iterator() {
					return new Iterator<String>() {
	
						String currentLine = null;
						
						public boolean hasNext() {
							if (currentLine == null)
								try {
									currentLine = reader.readLine();
								} catch (IOException e) {
								}
							return currentLine != null;
						}
	
						public String next() {
							if (currentLine != null) {
								String temp = currentLine;
								currentLine = null;
								return temp;
							}
							try {
								return reader.readLine();
							} catch (IOException e) {
								return null;
							}
						}
	
						public void remove() {
							throw new UnsupportedOperationException();
						}
					};
				}
			};
		} catch (FileNotFoundException e1) {
			throw new RuntimeException(e1);
		}
	}
}
