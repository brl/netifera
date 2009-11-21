package com.netifera.platform.ui.treemap.curves;

import org.eclipse.swt.graphics.GC;


public class XKCDHilbertCurve extends AbstractXKCDHilbertCurve {

	protected void paintRegions(int x, int y, int extent, GC gc) {
		drawRegion(x, y, extent, gc, new int[] {1,0, 0,1, -1,0});
		drawRegionLabel(x, y, extent, gc, "Local", 0,0, 1);
		
		drawRegion(x, y, extent, gc, new int[] {8,0, 0,4, 4,0, 0,-4});
		drawRegionLabel(x, y, extent, gc, "Multicast", 8,2, 4);

		drawRegion(x, y, extent, gc, new int[] {0,4, 0,3, 2,0, 0,-1, -1,0, 0,-2, -1,0});
		drawRegionLabel(x, y, extent, gc, "Asia-Pacific", 0,6, 2);

		drawRegion(x, y, extent, gc, new int[] {8,4, 0,1, 2,0, 0,1, 2,0, 0,-1, -1,0, 0,-1});
		drawRegionLabel(x, y, extent, gc, "Asia-Pacific", 8,4, 3);

		drawRegion(x, y, extent, gc, new int[] {8,4, 0,1, 2,0, 0,1, 2,0, 0,-1, -1,0, 0,-1});
		drawRegionLabel(x, y, extent, gc, "Asia-Pacific", 8,4, 3);

		drawRegion(x, y, extent, gc, new int[] {8,5, 0,1, 1,0, 0,-1});
		drawRegionLabel(x, y, extent, gc, "Europe", 8,5, 1);

		drawRegion(x, y, extent, gc, new int[] {9,6, 1,0});
		drawRegionLabel(x, y, extent, gc, "North America", 9,5, 1);

		drawRegion(x, y, extent, gc, new int[] {8,6, 0,1, 2,0, 0,-1});
		drawRegionLabel(x, y, extent, gc, "DoD", 8,6, 2);

		drawRegion(x, y, extent, gc, new int[] {8,7, 0,1, 2,0, 0,-1});
		drawRegionLabel(x, y, extent, gc, "Europe", 8,7, 2);

		drawRegion(x, y, extent, gc, new int[] {12,4, 0,2, 1,0, 0,-2, -1,0});
		drawRegionLabel(x, y, extent, gc, "Asia-Pacific", 12,4, 1);

		drawRegion(x, y, extent, gc, new int[] {13,4, 0,2, 1,0, 0,-2, -1,0});
		drawRegionLabel(x, y, extent, gc, "Latin America & Caribbean", 13,5, 1);

		drawRegion(x, y, extent, gc, new int[] {16,5, -1,0, 0,1, 1,0});
		drawRegionLabel(x, y, extent, gc, "Africa", 15,5, 1);

		drawRegion(x, y, extent, gc, new int[] {15,5, -1,0, 0,1, 1,0});
		drawRegionLabel(x, y, extent, gc, "North America", 14,5, 1);

		drawRegion(x, y, extent, gc, new int[] {14,4, 1,0, 0,1});
		drawRegionLabel(x, y, extent, gc, "US & Various", 14,4, 1);

		drawRegion(x, y, extent, gc, new int[] {14,8, 1,0, 0,-1, 1,0});
		drawRegionLabel(x, y, extent, gc, "Europe", 14,6, 2);

		drawRegion(x, y, extent, gc, new int[] {14,8, 0,1, 2,0});
		drawRegionLabel(x, y, extent, gc, "Various", 14,8, 2);

		drawRegion(x, y, extent, gc, new int[] {14,9, 0,1, 2,0});
		drawRegionLabel(x, y, extent, gc, "Latin America & Caribbean", 14,9, 2);
		
		drawRegion(x, y, extent, gc, new int[] {1,7, 0,1, 1,0, 0,-1});
		drawRegionLabel(x, y, extent, gc, "Europe", 1,7, 1);

		drawRegion(x, y, extent, gc, new int[] {2,8, 2,0, 0,4, -3,0, 0,-1, 1,0, 0,-1, -2,0});
		drawRegionLabel(x, y, extent, gc, "USA & Canada", 0,9, 4);

		drawRegion(x, y, extent, gc, new int[] {4,16, 0,-2, -2,0, 0,-2});
		drawRegionLabel(x, y, extent, gc, "Europe", 0,14, 4);

		drawRegion(x, y, extent, gc, new int[] {4,12, 0,2, 2,0, 0,-2, -2,0});
		drawRegionLabel(x, y, extent, gc, "North America", 4,12, 2);

		drawRegion(x, y, extent, gc, new int[] {4,10, 1,0, 0,-1, 1,0, 0,1, 1,0, 0,-2, -3,0});
		drawRegionLabel(x, y, extent, gc, "Asia-Pacific", 4,8, 3);

		drawRegion(x, y, extent, gc, new int[] {7,9, 1,0, 0,1, -1,0});
		drawRegionLabel(x, y, extent, gc, "Japan", 7,9, 1);

		drawRegion(x, y, extent, gc, new int[] {11,8, 1,0, 0,1});
		drawRegionLabel(x, y, extent, gc, "Japan", 11,8, 1);

		drawRegion(x, y, extent, gc, new int[] {11,6, 3,0, 0,2, -3,0, 0,-2});
		drawRegionLabel(x, y, extent, gc, "North America", 11,6, 3);

		drawRegion(x, y, extent, gc, new int[] {16,13, -1,0, 0,1, -1,0, 0,-2, -2,0, 0,-3, -1,0, 0,-1, -3,0, 0,8}); // various registrars
		drawRegionLabel(x, y, extent, gc, "Various Registries", 8,13, 6);

//		drawRegionLabel(x, y, extent, gc, "Public Data Network", 2,0, 1); // reclaimed in 2008
		drawRegionLabel(x, y, extent, gc, "HP", 3,0, 1);
		drawRegionLabel(x, y, extent, gc, "DEC", 4,0, 1);
		drawRegionLabel(x, y, extent, gc, "Ford", 5,0, 1);
		drawRegionLabel(x, y, extent, gc, "CSC", 6,0, 1);
		drawRegionLabel(x, y, extent, gc, "DDN-RYN", 7,0, 1);
		
		drawRegionLabel(x, y, extent, gc, "GE", 0,1, 1);
		drawRegionLabel(x, y, extent, gc, "Xerox", 2,1, 1);
		drawRegionLabel(x, y, extent, gc, "AT&T", 3,1, 1);
		drawRegionLabel(x, y, extent, gc, "Apple", 4,1, 1);
		drawRegionLabel(x, y, extent, gc, "MIT", 5,1, 1);
		drawRegionLabel(x, y, extent, gc, "DISA", 7,1, 1);
		
		drawRegionLabel(x, y, extent, gc, "Level3", 0,2, 1);
		drawRegionLabel(x, y, extent, gc, "Level3", 2,2, 1);
		drawRegionLabel(x, y, extent, gc, "DoD-Intel", 3,2, 1);
		drawRegionLabel(x, y, extent, gc, "DISA", 4,2, 2);
		drawRegionLabel(x, y, extent, gc, "Cable", 6,2, 1);
		drawRegionLabel(x, y, extent, gc, "UK MoD", 7,2, 1);

		drawRegionLabel(x, y, extent, gc, "US Army", 1,3, 1);
		drawRegionLabel(x, y, extent, gc, "IBM", 2,3, 1);
		drawRegionLabel(x, y, extent, gc, "Private", 3,3, 1);
		drawRegionLabel(x, y, extent, gc, "DSI", 5,3, 1);
		drawRegionLabel(x, y, extent, gc, "DISA", 7,3, 1);

		drawRegionLabel(x, y, extent, gc, "SITA", 1,4, 1);
		drawRegionLabel(x, y, extent, gc, "Merck", 2,4, 1);
		drawRegionLabel(x, y, extent, gc, "Cap Debis CCS", 3,4, 1);
		drawRegionLabel(x, y, extent, gc, "AT&T", 4,4, 1);
		drawRegionLabel(x, y, extent, gc, "MERIT", 5,4, 1);

		drawRegionLabel(x, y, extent, gc, "USPS", 1,5, 1);
		drawRegionLabel(x, y, extent, gc, "DoD NIC", 2,5, 1);
		drawRegionLabel(x, y, extent, gc, "duPont", 3,5, 1);
		drawRegionLabel(x, y, extent, gc, "DLA", 4,5, 1);
		drawRegionLabel(x, y, extent, gc, "Halliburton", 5,5, 1);
		drawRegionLabel(x, y, extent, gc, "PSI", 7,5, 1);

		drawRegionLabel(x, y, extent, gc, "UK Department for Work and Pensions", 3,6, 1);
//		drawRegionLabel(x, y, extent, gc, "BB&N", 4,6, 1); // was reclaimed, now unallocated
		drawRegionLabel(x, y, extent, gc, "INTEROP", 5,6, 1);
		drawRegionLabel(x, y, extent, gc, "Eli Lily", 6,6, 1);
		drawRegionLabel(x, y, extent, gc, "AfriNIC", 7,6, 1);

		drawRegionLabel(x, y, extent, gc, "Prudential", 3,7, 1);
		drawRegionLabel(x, y, extent, gc, "Bell Northern Research", 4,7, 1);
		drawRegionLabel(x, y, extent, gc, "HAM Radio", 5,7, 1);
		drawRegionLabel(x, y, extent, gc, "Japan", 6,7, 1);

		drawRegionLabel(x, y, extent, gc, "Loopback", 7,8, 1);
	}
}