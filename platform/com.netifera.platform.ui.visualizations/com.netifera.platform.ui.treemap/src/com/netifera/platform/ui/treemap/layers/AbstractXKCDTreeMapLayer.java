package com.netifera.platform.ui.treemap.layers;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;

import com.netifera.platform.ui.treemap.ITreeMapLayerProvider;
import com.netifera.platform.util.addresses.inet.IPv4Address;
import com.netifera.platform.util.addresses.inet.IPv4Netblock;


public abstract class AbstractXKCDTreeMapLayer implements ITreeMapLayerProvider {

	private static int[] inverseCurve = {
		0,1,14,15,16,19,20,21,234,235,236,239,240,241,254,255,
		3,2,13,12,17,18,23,22,233,232,237,238,243,242,253,252,
		4,7,8,11,30,29,24,25,230,231,226,225,244,247,248,251,
		5,6,9,10,31,28,27,26,229,228,227,224,245,246,249,250,
		58,57,54,53,32,35,36,37,218,219,220,223,202,201,198,197,
		59,56,55,52,33,34,39,38,217,216,221,222,203,200,199,196,
		60,61,50,51,46,45,40,41,214,215,210,209,204,205,194,195,
		63,62,49,48,47,44,43,42,213,212,211,208,207,206,193,192,
		64,67,68,69,122,123,124,127,128,131,132,133,186,187,188,191,
		65,66,71,70,121,120,125,126,129,130,135,134,185,184,189,190,
		78,77,72,73,118,119,114,113,142,141,136,137,182,183,178,177,
		79,76,75,74,117,116,115,112,143,140,139,138,181,180,179,176,
		80,81,94,95,96,97,110,111,144,145,158,159,160,161,174,175,
		83,82,93,92,99,98,109,108,147,146,157,156,163,162,173,172,
		84,87,88,91,100,103,104,107,148,151,152,155,164,167,168,171,
		85,86,89,90,101,102,105,106,149,150,153,154,165,166,169,170
	};

	private static int[] curve = new int[256];

	static {
		for (int i=0; i<=255; i++)
			curve[inverseCurve[i]] = i;
	}
	
	/*
0: Local
1-2: Unallocated
3: General Electric
4: BB&N INC
5: Unallocated
6: Army misc
7: Unallocated
8: BB&N INC
9: IBM
10: VPNs
11: DoD intel
12: Bell Labs
13: Xerox
14: Public data nets
15: HP
16: DEC
17: Apple
18: MIT
19: Ford
20: CSC
21: DDN-RYN
22: DISA
23: Unallocated
24: Cable TV
25: UK MoD
26: DISA
27: Unallocated
28: DSI
29-30: DISA
31: Unallocated
32: Norsk
33: DLA
34: Halliburton
35: Merit
36-37: Unallocated
38: PSI
39: Unallocated
40: Eli Lily
41: ARINIC
42: Unallocated
43: Japan INET
44: HAM Radio
45: INTEROP
46: BB&N INC
47: Bell North
48: Prudential
49-50: Unallocated
51: UK Social Security
52: duPont
53: CAP DEBIS CCS
54: Merc
55: Boeing
56: USPS
57: SITA
58-61: Asia-Pacific
62: Europe
63-76: USA & Canada (contains: UUNET, Google, Digg, The Onion, Slashdot, Ebay, Craigslist, Flickr, Orisinal, xkcd)
77-79: Europe (unused)
80-91: Europe
92-95: Unallocated
96-99: North America
100-120: Unallocated
121-125: Asia-Pacific
126: Japan
127: Loopback
128-132: Various Registrars
133: Japan
134-172: Various Registrars
173-187: Unallocated
188: Various
189-190: Latin America & Caribbean
191-192: Various (contains Private (RFC 1918))
193-195: Europe
196: Africa
197: Unallocated
198: US & Various
199: North America
200-201: Latin America & Caribbean
202-203: Asia-Pacific (contains Karei.co.jp)
204-209: North America (contains SuicideGirls and BoingBoing)
210-211: Asia-Pacific
212-213: Europe
214-215: US Department of Defense
216: North America (contains MySpace and SomethingAwful)
217: Europe
218-222: Asia-Pacific
223: Unallocated
224-239: Multicast
240-255: Unallocated
	*/
	
	public int getIndex(IPv4Netblock netblock, IPv4Netblock subnetblock) {
		return curve[subnetblock.getNetworkAddress().toBytes()[netblock.getCIDR()/8] & 0xff];
	}

	public IPv4Netblock getSubNetblock(IPv4Netblock netblock, int index) {
		IPv4Address subnetAddress = new IPv4Address(netblock.getNetworkAddress().toInteger() | (inverseCurve[index] << (24-netblock.getCIDR())));
		return new IPv4Netblock(subnetAddress, netblock.getCIDR()+8);
	}

	protected void drawRegion(int x, int y, int extent, GC gc, int[] coordinatesArray) {
		gc.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_GRAY));
		gc.setAlpha(255);
		int[] pointArray = new int[coordinatesArray.length];
		int xi = coordinatesArray[0];
		int yi = coordinatesArray[1];
		pointArray[0] = x + (xi*extent/16);
		pointArray[1] = y + (yi*extent/16);
		for (int i=2; i < coordinatesArray.length; i += 2) {
			xi += coordinatesArray[i];
			yi += coordinatesArray[i+1];
			pointArray[i] = x + (xi*extent/16);
			pointArray[i+1] = y + (yi*extent/16);
		}
		gc.drawPolyline(pointArray);
	}

	protected void drawRegionLabel(int x, int y, int extent, GC gc, String label, int i, int j, int w) {
		int availableSpace = w*extent/16;
		if (availableSpace < 4)
			return;
		int fontSize = Math.min(availableSpace/label.length(), extent/20 /*extent/16*/);
		if (fontSize <= 0 || fontSize >= 150)
			return;
		gc.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		if (fontSize <= 10)
			gc.setAlpha(14*fontSize);
		else
			gc.setAlpha(150-fontSize);
		Font font = new Font(Display.getDefault(),"Arial",fontSize,SWT.BOLD);
		gc.setFont(font);
		Point labelExtent = gc.stringExtent(label);
/*		if (labelExtent.x < availableSpace-(Math.max(4, availableSpace/8))) {
			font.dispose();
			fontSize = fontSize *
		}
*/		int labelX = x + (i*extent/16) + (availableSpace - labelExtent.x)/2;
		int labelY = y + (j*extent/16) + (extent/16 - labelExtent.y)/2;
		gc.drawString(label, labelX, labelY, true);
		font.dispose();
	}

	private int hxhy2i(int hx, int hy) {
		int h = hy*16 + hx;
		return inverseCurve[h];
	}
	
	private int coverCategory(String category, Map<Point,Integer> indices, String[] categories, int hx, int hy) {
		if (hx < 0 || hx > 15 || hy < 0 || hy > 15)
			return 0;
		int i = hxhy2i(hx,hy);
		if (categories[i] == null || categories[i] != category)
			return 0;
		categories[i] = null;
		int length = coverCategory(category, indices, categories, hx+1, hy)+1;
		indices.put(new Point(hx, hy), length);
		//FIXME this is covering all the connected region of this category, but is not properly computing the points of maximum length
		coverCategory(category, indices, categories, hx-1, hy);
		coverCategory(category, indices, categories, hx, hy-1);
		coverCategory(category, indices, categories, hx, hy+1);
		return length;
	}
	
	private void drawCategories(int x, int y, int extent, GC gc, String[] categories) {
		gc.setAlpha(Math.min(extent/16, 128)); // make the category borders gradually appear as we zoom-in
		for (int hx=0; hx<16; hx++) {
			for (int hy=0; hy<16; hy++) {
				String category = categories[hxhy2i(hx,hy)];
				int xi = x + (hx*extent/16);
				int yi = y + (hy*extent/16);
				int xiPlus1 = x + ((hx+1)*extent/16);
				int yiPlus1 = y + ((hy+1)*extent/16);
				if (hx<15 && category != categories[hxhy2i(hx+1,hy)])
					gc.drawLine(xiPlus1, yi, xiPlus1, yiPlus1);
				if (hy<15 && category != categories[hxhy2i(hx,hy+1)])
					gc.drawLine(xi, yiPlus1, xiPlus1, yiPlus1);
				if (category != null) {
					if (hx == 0)
						gc.drawLine(xi, yi, xi, yiPlus1);
					if (hx == 15)
						gc.drawLine(xiPlus1, yi, xiPlus1, yiPlus1);
					if (hy == 0)
						gc.drawLine(xi, yi, xiPlus1, yi);
					if (hy == 15)
						gc.drawLine(xi, yiPlus1, xiPlus1, yiPlus1);
				}
			}
		}
		for (int hy=0; hy<16; hy++) {
			for (int hx=0; hx<16; hx++) {
				int i = hxhy2i(hx,hy);
				String category = categories[i];
				if (category != null) {
					Map<Point,Integer> indices = new HashMap<Point,Integer>();
					coverCategory(category,indices,categories,hx,hy);
					int maxLength = 0;
					Point bestPoint = null;
					for (Point point: indices.keySet()) {
						int length = indices.get(point);
						if (length > maxLength) {
							maxLength = length;
							bestPoint = point;
						}
					}
					drawRegionLabel(x, y, extent, gc, category, bestPoint.x, bestPoint.y, maxLength);
				}
			}
		}
	}
	
	private void paintTopLevelRegions(int x, int y, int extent, GC gc) {
		drawRegion(x, y, extent, gc, new int[] {1,0, 0,1, -1,0});
		drawRegionLabel(x, y, extent, gc, "Local", 0,0, 1);
		
		drawRegion(x, y, extent, gc, new int[] {8,0, 0,4, 4,0, 0,-4});
		drawRegionLabel(x, y, extent, gc, "Multicast", 8,2, 4);

		drawRegion(x, y, extent, gc, new int[] {0,4, 0,3, 2,0, 0,-1, -1,0, 0,-2, -1,0});
		drawRegionLabel(x, y, extent, gc, "APNIC", 0,6, 2);

		drawRegion(x, y, extent, gc, new int[] {8,4, 0,1, 2,0, 0,1, 2,0, 0,-1, -1,0, 0,-1});
		drawRegionLabel(x, y, extent, gc, "APNIC", 8,4, 3);

		drawRegion(x, y, extent, gc, new int[] {8,4, 0,1, 2,0, 0,1, 2,0, 0,-1, -1,0, 0,-1});
		drawRegionLabel(x, y, extent, gc, "APNIC", 8,4, 3);

		drawRegion(x, y, extent, gc, new int[] {8,5, 0,1, 1,0, 0,-1});
		drawRegionLabel(x, y, extent, gc, "RIPE", 8,5, 1);

		drawRegion(x, y, extent, gc, new int[] {9,6, 1,0});
		drawRegionLabel(x, y, extent, gc, "ARIN", 9,5, 1);

		drawRegion(x, y, extent, gc, new int[] {8,6, 0,1, 2,0, 0,-1});
		drawRegionLabel(x, y, extent, gc, "DoD", 8,6, 2);

		drawRegion(x, y, extent, gc, new int[] {8,7, 0,1, 2,0, 0,-1});
		drawRegionLabel(x, y, extent, gc, "RIPE", 8,7, 2);

		drawRegion(x, y, extent, gc, new int[] {12,4, 0,2, 1,0, 0,-2, -1,0});
		drawRegionLabel(x, y, extent, gc, "APNIC", 12,4, 1);

		drawRegion(x, y, extent, gc, new int[] {13,4, 0,2, 1,0, 0,-2, -1,0});
		drawRegionLabel(x, y, extent, gc, "LACNIC", 13,5, 1);

		drawRegion(x, y, extent, gc, new int[] {16,5, -1,0, 0,1, 1,0});
		drawRegionLabel(x, y, extent, gc, "AfriNIC", 15,5, 1);

		drawRegion(x, y, extent, gc, new int[] {15,5, -1,0, 0,1, 1,0});
		drawRegionLabel(x, y, extent, gc, "ARIN", 14,5, 1);

		drawRegion(x, y, extent, gc, new int[] {14,4, 1,0, 0,1});
		drawRegionLabel(x, y, extent, gc, "US & Various", 14,4, 1);

		drawRegion(x, y, extent, gc, new int[] {14,8, 1,0, 0,-1, 1,0});
		drawRegionLabel(x, y, extent, gc, "RIPE", 14,6, 2);

		drawRegion(x, y, extent, gc, new int[] {14,8, 0,1, 2,0});
		drawRegionLabel(x, y, extent, gc, "Various", 14,8, 2);

		drawRegion(x, y, extent, gc, new int[] {14,9, 0,1, 2,0});
		drawRegionLabel(x, y, extent, gc, "LACNIC", 14,9, 2);
		
		drawRegion(x, y, extent, gc, new int[] {1,7, 0,1, 1,0, 0,-1});
		drawRegionLabel(x, y, extent, gc, "RIPE", 1,7, 1);

		drawRegion(x, y, extent, gc, new int[] {2,8, 2,0, 0,4, -3,0, 0,-1, 1,0, 0,-1, -2,0});
		drawRegionLabel(x, y, extent, gc, "ARIN", 0,9, 4);

		drawRegion(x, y, extent, gc, new int[] {4,16, 0,-2, -2,0, 0,-2});
		drawRegionLabel(x, y, extent, gc, "RIPE", 0,14, 4);

		drawRegion(x, y, extent, gc, new int[] {4,12, 0,2, 2,0, 0,-2, -2,0});
		drawRegionLabel(x, y, extent, gc, "ARIN", 4,12, 2);

		drawRegion(x, y, extent, gc, new int[] {4,10, 1,0, 0,-1, 1,0, 0,1, 1,0, 0,-2, -3,0});
		drawRegionLabel(x, y, extent, gc, "APNIC", 4,8, 3);

		drawRegion(x, y, extent, gc, new int[] {7,9, 1,0, 0,1, -1,0});
		drawRegionLabel(x, y, extent, gc, "APNIC", 7,9, 1);

		drawRegion(x, y, extent, gc, new int[] {11,8, 1,0, 0,1});
		drawRegionLabel(x, y, extent, gc, "APNIC", 11,8, 1);

		drawRegion(x, y, extent, gc, new int[] {11,6, 3,0, 0,2, -3,0, 0,-2});
		drawRegionLabel(x, y, extent, gc, "ARIN", 11,6, 3);

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
		drawRegionLabel(x, y, extent, gc, "DoD/Intel", 3,2, 1);
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
		drawRegionLabel(x, y, extent, gc, "APNIC", 6,7, 1);

		drawRegionLabel(x, y, extent, gc, "Loopback", 7,8, 1);
	}
	
	public void paint(int x, int y, int extent, GC gc, IPv4Netblock netblock) {
		if (newCache.size() > 4096) {
			oldCache = newCache;
			newCache = new HashMap<IPv4Netblock,String>();
			uniqueStrings = new HashMap<String,String>();
		}
		if (netblock.getCIDR() == 0) {
			gc.setAlpha(50);
			gc.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY));
			gc.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY));
			for (int i: new int[] {1, 2, 5, 7, 23, 27, 31, 36, 37, 39, 42, 46, 49, 50, 92, 93, 94, 95, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 173, 174, 175, 176, 177, 178, 179, 180, 181, 182, 183, 184, 185, 186, 187, 197, 223, 240, 241, 242, 243, 244, 245, 246, 247, 248, 249, 250, 251, 252, 253, 254, 255}) {
				int h = curve[i];
				int hx = h % 16;
				int hy = h / 16;
				int xi = x + (hx*extent/16);
				int yi = y + (hy*extent/16);
				gc.fillRectangle(xi, yi, extent/16+1, extent/16+1);
			}
			
			gc.setLineWidth(1);
	
			paintTopLevelRegions(x, y, extent, gc);
		} else if (netblock.getCIDR() == 8) {
			gc.setAlpha(150);
			String[] categories = new String[256];
			int net = netblock.getNetworkAddress().toInteger();
			for (int i=0; i<=255; i++) {
				net = (net & 0xff000000) | ((i & 0xff) << 16);
				IPv4Netblock subnetblock = new IPv4Netblock(new IPv4Address(net),16);
				categories[i] = getCacheCategory(subnetblock);
			}
			drawCategories(x, y, extent, gc, categories);
		} else if (netblock.getCIDR() == 16) {
			gc.setAlpha(150);
			String[] categories = new String[256];
			int net = netblock.getNetworkAddress().toInteger();
			for (int i=0; i<=255; i++) {
				net = (net & 0xffff0000) | ((i & 0xff) << 8);
				IPv4Netblock subnetblock = new IPv4Netblock(new IPv4Address(net),24);
				categories[i] = getCacheCategory(subnetblock);
			}
			drawCategories(x, y, extent, gc, categories);
		} else if (netblock.getCIDR() == 24) {
			// organization?
		}
	}

	private Map<IPv4Netblock,String> oldCache = new HashMap<IPv4Netblock,String>();
	private Map<IPv4Netblock,String> newCache = new HashMap<IPv4Netblock,String>();
	private Map<String,String> uniqueStrings = new HashMap<String,String>();
	
	private String getCacheCategory(IPv4Netblock netblock) {
		String category = oldCache.get(netblock);
		if (category == null)
			category = newCache.get(netblock);
		if (category == null)
			category = getCategory(netblock);
		if (category != null) {
			String unique = uniqueStrings.get(category);
			if (unique == null) uniqueStrings.put(category, category);
			else category = unique;
		}
		newCache.put(netblock, category);
		return category;
	}
	
	abstract protected String getCategory(IPv4Netblock netblock);
}
