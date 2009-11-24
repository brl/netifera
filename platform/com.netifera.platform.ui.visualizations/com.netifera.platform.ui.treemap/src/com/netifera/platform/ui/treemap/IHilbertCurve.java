package com.netifera.platform.ui.treemap;

import org.eclipse.swt.graphics.GC;

import com.netifera.platform.util.addresses.inet.IPv4Netblock;

public interface IHilbertCurve {
	String getName();
	int getIndex(IPv4Netblock netblock, IPv4Netblock subnetblock);
	IPv4Netblock getSubNetblock(IPv4Netblock netblock, int index);
	void paint(int x, int y, int extent, GC gc, IPv4Netblock netblock);
}
