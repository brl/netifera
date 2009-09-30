package com.netifera.platform.net.http.spider.impl;

import java.util.BitSet;

public class BloomFilter {

	private BitSet bits;
	private int count = 0;
	
	public BloomFilter(int bitSize) {
		bits = new BitSet(bitSize);
	}
	
	public boolean add(String value) {
		int i = indexOf(0, value);
		if (bits.get(i))
			return true;
		bits.set(i);
		count = count + 1;
		return false;
	}
	
	public boolean contains(String value) {
		int i = indexOf(0, value);
		return bits.get(i);
	}
	
	private long hashOf(int k, String value) {
		long hash = 5381;
		for (int i = 0; i < value.length(); i++) {
			hash = ((hash << 5) + hash) + value.charAt(i);
		}
		return hash;
	}
	
	private int indexOf(int k, String value) {
		long hash = hashOf(k, value);
		return (int) ((hash < 0 ? -hash : hash) % bits.size());
	}

	public int capacity() {
		return bits.size();
	}
	
	public int size() {
		return count;
	}
	
	public double falsePositiveProbability() {
		double k = 1;
		double n = size();
		double m = capacity();
		return Math.pow(1 - Math.pow(1.0 - 1.0/m, k*n), k);
	}
}
