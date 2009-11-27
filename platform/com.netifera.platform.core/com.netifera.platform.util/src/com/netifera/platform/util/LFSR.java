package com.netifera.platform.util;

public class LFSR {

	final private int mask;
	final private int taps;
	
	private int register = 1;

	public LFSR(int n) {
		mask = makeMask(n);
		taps = makeTaps(n);
	}
	
	public void set(int value) {
		register = value;
	}
	
	public int get() {
		return register;
	}
	
	public int next() {
		// register & 1 selects the low order bit, and do - to create an int that is all 1's if was 1, or all 0's if was 0; the rest is easy to understand
		register = ((register >>> 1) ^ (-(register & 1) & taps)) & mask;
		return get();
	}
	
	/** Generates a mask that will preserve the n low order bits and put 0's into the high order bits. */
	private static int makeMask(int n) {
		int m = 0;
		for (int i = 0; i < n; i++) m |= 1 << i;
		return m;
	}
	
	/** There is no easy algorithm to generate the taps as a function of n; instead simply have to rely on tables. */
	private static int makeTaps(int n) throws IllegalArgumentException {
		switch (n) {
			case 3: return (1 << 2) | (1 << 1);
			case 4: return (1 << 3) | (1 << 2);	// i.e. 4 3
			case 5: return (1 << 4) | (1 << 2);	// i.e. 5 3
			case 6: return (1 << 5) | (1 << 4);	// i.e. 6 5
			case 7: return (1 << 6) | (1 << 5);	// i.e. 7 6
			case 8: return (1 << 7) | (1 << 5) | (1 << 4) | (1 << 3);	// i.e. 8 6 5 4
			case 9: return (1 << 8) | (1 << 4);	// i.e. 9 5
			case 10: return (1 << 9) | (1 << 6);	// i.e. 10 7
			case 11: return (1 << 10) | (1 << 8);	// i.e. 11 9
			case 12: return (1 << 11) | (1 << 10) | (1 << 9) | (1 << 3);	// i.e. 12 11 10 4
			case 13: return (1 << 12) | (1 << 11) | (1 << 10) | (1 << 7);	// i.e. 13 12 11 8
			case 14: return (1 << 13) | (1 << 12) | (1 << 11) | (1 << 1);	// i.e. 14 13 12 2
			case 15: return (1 << 14) | (1 << 13);	// i.e. 15 14
			case 16: return (1 << 15) | (1 << 13) | (1 << 12) | (1 << 10);	// i.e. 16 14 13 11
			case 17: return (1 << 16) | (1 << 13);	// i.e. 17 14
			case 18: return (1 << 17) | (1 << 10);	// i.e. 18 11
			case 19: return (1 << 18) | (1 << 17) | (1 << 16) | (1 << 13);	// i.e. 19 18 17 14
			case 20: return (1 << 19) | (1 << 16);
			case 21: return (1 << 20) | (1 << 18);
			case 22: return (1 << 21) | (1 << 20);
			case 23: return (1 << 22) | (1 << 17);
			case 24: return (1 << 23) | (1 << 22) | (1 << 21) | (1 << 16);
			case 25: return (1 << 24) | (1 << 21);
			case 26: return (1 << 25) | (1 << 5) | (1 << 1) | (1 << 0);
			case 27: return (1 << 26) | (1 << 4) | (1 << 1) | (1 << 0);
			case 28: return (1 << 27) | (1 << 24);
			case 29: return (1 << 28) | (1 << 26);
			case 30: return (1 << 29) | (1 << 5) | (1 << 3) | (1 << 0);
			case 31: return (1 << 30) | (1 << 27);	// i.e. 31 28
			case 32: return (1 << 31) | (1 << 30) | (1 << 28) | (1 << 0);	// i.e. 32 31 29 1
	 
			default: throw new IllegalArgumentException("Unknown primitive polynomial for n = "+n);
		}
	}
}
