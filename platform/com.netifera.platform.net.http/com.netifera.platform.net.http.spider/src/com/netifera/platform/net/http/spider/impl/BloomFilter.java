package com.netifera.platform.net.http.spider.impl;

import java.util.BitSet;

public class BloomFilter {

	private BitSet bits;
	
	public BloomFilter(int bitSize) {
		bits = new BitSet(bitSize);
	}
	
	public boolean add(String value) {
		int i = indexOf(value);
		if (bits.get(i))
			return true;
		bits.set(i);
		return false;
	}
	
	public boolean contains(String value) {
		int i = indexOf(value);
		return bits.get(i);
	}
	
	private int indexOf(String value) {
		long hash = 5381;

		for (int i = 0; i < value.length(); i++) {
			hash = ((hash << 5) + hash) + value.charAt(i);
		}

		return (int) ((hash < 0 ? -hash : hash) % bits.size());
	}
}
