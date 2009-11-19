package com.netifera.platform.ui.treemap;

import org.eclipse.swt.graphics.GC;

import com.netifera.platform.util.addresses.inet.IPv4Netblock;

public interface IHilbertCurve {
	String getName();
	int getIndex(IPv4Netblock netblock, IPv4Netblock subnetblock);
	void paint(int x, int y, int extent, GC gc);
}
