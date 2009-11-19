package com.netifera.platform.ui.treemap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;

import com.netifera.platform.util.addresses.inet.IPv4Netblock;


public class DefaultHilbertCurve implements IHilbertCurve {

	private static int[] hilbertCurve = new int[256];
	private static String[] regions = new String[256];
	
	static {
		int[] reverseMapping = {0,1,14,15,16,19,20,21,234,235,236,239,240,241,254,255,
		3,2,13,12,17,18,23,22,233,232,237,238,243,242,253,252,
		4,7,8,11,30,29,24,25,230,231,226,225,244,247,248,251,
		5,6,9,10,31,28,27,26,229,228,227,224,245,246,249,250,
		58,57,54,53,32,35,36,37,218,219,220,223,202,201,198,197,
		59,56,55,52,33,34,39,38,217,216,221,222,203,200,199,196,
		60,61,50,51,46,45,40,41,214,215,210,209,204,205,194,195,
		63,62,49,48,47,44,43,42,213,212,211,208,207,206,193,192,
		64,67,68,69,122,123,124,127,128,131,132,133,186,187,188,191,
		65,66,71,70,121,120,125,126,129,130,135,185,184,189,190,
		78,77,72,73,118,119,114,113,142,141,136,137,182,183,178,177,
		79,76,75,74,117,116,115,112,143,140,139,138,181,180,179,176,
		80,81,94,95,96,97,110,111,144,145,158,159,160,161,174,175,
		83,82,93,92,99,98,109,108,147,146,157,156,163,162,173,172,
		84,87,88,91,100,103,104,107,148,151,152,155,164,167,168,171,
		85,86,89,90,101,102,105,106,149,150,153,154,165,166,169,170};

		for (int i=0; i<255; i++)
			hilbertCurve[reverseMapping[i]] = i;

		
		regions[0] = "Local";
		for (int i=1; i<=2; i++) regions[i] = "Unallocated";
		regions[3] = "General Electric";
		regions[4] = "BB&N INC";
		regions[5] = "Unallocated";
		regions[6] = "Army misc";
		regions[7] = "Unallocated";
		regions[8] = "BB&N INC";
		regions[9] = "IBM";
		regions[10] = "VPNs";
		regions[11] = "DoD intel";
		regions[12] = "Bell Labs";
		regions[13] = "Xerox";
		regions[14] = "Public data nets";
		regions[15] = "HP";
		regions[16] = "DEC";
		regions[17] = "Apple";
		regions[18] = "MIT";
		regions[19] = "Ford";
		regions[20] = "CSC";
		regions[21] = "DDN-RYN";
		regions[22] = "DISA";
		regions[23] = "Unallocated";
		regions[24] = "Cable TV";
		regions[25] = "UK MoD";
		regions[26] = "DISA";
		regions[27] = "Unallocated";
		regions[28] = "DSI";
		regions[29] = "DISA";
		regions[30] = "DISA";
		regions[31] = "Unallocated";
		regions[32] = "Norsk";
		regions[33] = "DLA";
		regions[34] = "Halliburton";
		regions[35] = "Merit";
		for (int i=36; i<=37; i++) regions[i] = "Unallocated";
		regions[38] = "PSI";
		regions[39] = "Unallocated";
		regions[40] = "Eli Lily";
		regions[41] = "ARINIC";
		regions[42] = "Unallocated";
		regions[43] = "Japan INET";
		regions[44] = "HAM Radio";
		regions[45] = "INTEROP";
		regions[46] = "BB&N INC";
		regions[47] = "Bell North";
		regions[48] = "Prudential";
		for (int i=49; i<=50; i++) regions[i] = "Unallocated";
		regions[51] = "UK Social Security";
		regions[52] = "duPont";
		regions[53] = "CAP DEBIS CCS";
		regions[54] = "Merc";
		regions[55] = "Boeing";
		regions[56] = "USPS";
		regions[57] = "SITA";
		for (int i=58; i<=61; i++) regions[i] = "Asia-Pacific";
		regions[62] = "Europe";
		for (int i=63; i<=76; i++) regions[i] = "USA & Canada (contains: UUNET, Google, Digg, The Onion, Slashdot, Ebay, Craigslist, Flickr, Orisinal, xkcd)";
		for (int i=77; i<=79; i++) regions[i] = "Europe (unused)";
		for (int i=80; i<=91; i++) regions[i] = "Europe";
		for (int i=92; i<=95; i++) regions[i] = "Unallocated";
		for (int i=96; i<=99; i++) regions[i] = "North America";
		for (int i=100; i<=120; i++) regions[i] = "Unallocated";
		for (int i=121; i<=125; i++) regions[i] = "Asia-Pacific";
		regions[126] = "Japan";
		regions[127] = "Loopback";
		for (int i=128; i<=132; i++) regions[i] = "Various Registrars";
		regions[133] = "Japan";
		for (int i=134; i<=172; i++) regions[i] = "Various Registrars";
		for (int i=173; i<=187; i++) regions[i] = "Unallocated";
		regions[188] = "Various";
		for (int i=189; i<=190; i++) regions[i] = "Latin America & Caribbean";
		for (int i=191; i<=192; i++) regions[i] = "Various (contains Private (RFC 1918))";
		for (int i=193; i<=195; i++) regions[i] = "Europe";
		regions[196] = "Africa";
		regions[197] = "Unallocated";
		regions[198] = "US & Various";
		regions[199] = "North America";
		for (int i=200; i<=201; i++) regions[i] = "Latin America & Caribbean";
		for (int i=202; i<=203; i++) regions[i] = "Asia-Pacific (contains Karei.co.jp)";
		for (int i=204; i<=209; i++) regions[i] = "North America (contains SuicideGirls and BoingBoing)";
		for (int i=210; i<=211; i++) regions[i] = "Asia-Pacific";
		for (int i=212; i<=213; i++) regions[i] = "Europe";
		for (int i=214; i<=215; i++) regions[i] = "US Department of Defense";
		regions[216] = "North America (contains MySpace and SomethingAwful)";
		regions[217] = "Europe";
		for (int i=218; i<=222; i++) regions[i] = "Asia-Pacific";
		regions[223] = "Unallocated";
		for (int i=224; i<=239; i++) regions[i] = "Multicast";
		for (int i=240; i<=255; i++) regions[i] = "Unallocated";
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
	
	public String getName() {
		return "Default";
	}

	public int getIndex(IPv4Netblock netblock, IPv4Netblock subnetblock) {
		return hilbertCurve[subnetblock.getNetworkAddress().toBytes()[netblock.getCIDR()/8] & 0xff];
	}

	private void drawRegion(int x, int y, int extent, GC gc, int[] coordinatesArray) {
		gc.setAlpha(150);
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

	private void drawRegionLabel(int x, int y, int extent, GC gc, String label, int i, int j, int w) {
		int fontSize = w*extent/16/label.length();
		if (fontSize >= 150)
			return;
		gc.setAlpha(150-fontSize);
		Font font = new Font(Display.getDefault(),"Arial",fontSize,SWT.BOLD);
		gc.setFont(font);
		Point labelExtent = gc.stringExtent(label);
		int labelX = x + (i*extent/16) + (w*extent/16 - labelExtent.x)/2;
		int labelY = y + (j*extent/16) + (extent/16 - labelExtent.y)/2;
		gc.drawString(label, labelX, labelY, true);
		font.dispose();
	}
	
	public void paint(int x, int y, int extent, GC gc) {
		gc.setAlpha(50);
		gc.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY));
		gc.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY));
		for (int i: new int[] {1, 2, 5, 7, 23, 27, 31, 36, 37, 39, 42, 49, 50, 92, 93, 94, 95, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 173, 174, 175, 176, 177, 178, 179, 180, 181, 182, 183, 184, 185, 186, 187, 197, 223, 240, 241, 242, 243, 244, 245, 246, 247, 248, 249, 250, 251, 252, 253, 254, 255}) {
			int h = hilbertCurve[i];
			int xi = h % 16;
			int yi = h / 16;
			int x1 = x + (xi*extent/16);
			int y1 = y + (yi*extent/16);
			gc.fillRectangle(x1, y1, extent/16+1, extent/16+1);
		}
		
		gc.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		gc.setLineWidth(2);
		
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
		drawRegionLabel(x, y, extent, gc, "Europe", 8,5, 1);

		drawRegion(x, y, extent, gc, new int[] {9,6, 1,0});
		drawRegionLabel(x, y, extent, gc, "North America", 9,5, 1);

		drawRegion(x, y, extent, gc, new int[] {8,6, 0,1, 2,0, 0,-1});
		drawRegionLabel(x, y, extent, gc, "DoD", 8,6, 2);

		drawRegion(x, y, extent, gc, new int[] {8,7, 0,1, 2,0, 0,-1});
		drawRegionLabel(x, y, extent, gc, "Europe", 8,7, 2);

		drawRegion(x, y, extent, gc, new int[] {12,4, 0,2, 1,0, 0,-2, -1,0});
		drawRegionLabel(x, y, extent, gc, "APNIC", 12,4, 1);

		drawRegion(x, y, extent, gc, new int[] {13,4, 0,2, 1,0, 0,-2, -1,0});
		drawRegionLabel(x, y, extent, gc, "LACNIC", 13,5, 1);

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
		drawRegionLabel(x, y, extent, gc, "LACNIC", 14,9, 2);
		
		drawRegion(x, y, extent, gc, new int[] {1,7, 0,1, 1,0, 0,-1});
		drawRegionLabel(x, y, extent, gc, "Europe", 1,7, 1);

		drawRegion(x, y, extent, gc, new int[] {2,8, 2,0, 0,4, -3,0, 0,-1, 1,0, 0,-1, -2,0});
		drawRegionLabel(x, y, extent, gc, "USA & Canada", 0,9, 4);

		drawRegion(x, y, extent, gc, new int[] {4,16, 0,-2, -2,0, 0,-2});
		drawRegionLabel(x, y, extent, gc, "Europe", 0,14, 4);

		drawRegion(x, y, extent, gc, new int[] {4,12, 0,2, 2,0, 0,-2, -2,0});
		drawRegionLabel(x, y, extent, gc, "North America", 4,12, 2);

		drawRegion(x, y, extent, gc, new int[] {4,10, 1,0, 0,-1, 1,0, 0,1, 1,0, 0,-2, -3,0});
		drawRegionLabel(x, y, extent, gc, "APNIC", 4,8, 3);

		drawRegion(x, y, extent, gc, new int[] {7,9, 1,0, 0,1, -1,0});
		drawRegionLabel(x, y, extent, gc, "Japan", 7,9, 1);

		drawRegion(x, y, extent, gc, new int[] {11,8, 1,0, 0,1});
		drawRegionLabel(x, y, extent, gc, "Japan", 11,8, 1);

		drawRegion(x, y, extent, gc, new int[] {11,6, 3,0, 0,2, -3,0, 0,-2});
		drawRegionLabel(x, y, extent, gc, "North America", 11,6, 3);

		drawRegion(x, y, extent, gc, new int[] {16,13, -1,0, 0,1, -1,0, 0,-2, -2,0, 0,-3, -1,0, 0,-1, -3,0, 0,8}); // various registrars
		drawRegionLabel(x, y, extent, gc, "Various Registries", 8,13, 6);

		drawRegionLabel(x, y, extent, gc, "Public Data Nets", 2,0, 1);
		drawRegionLabel(x, y, extent, gc, "HP", 3,0, 1);
		drawRegionLabel(x, y, extent, gc, "DEC", 4,0, 1);
		drawRegionLabel(x, y, extent, gc, "Ford", 5,0, 1);
		drawRegionLabel(x, y, extent, gc, "CSC", 6,0, 1);
		drawRegionLabel(x, y, extent, gc, "DDN-RYN", 7,0, 1);
		
		drawRegionLabel(x, y, extent, gc, "GE", 0,1, 1);
		drawRegionLabel(x, y, extent, gc, "Xerox", 2,1, 1);
		drawRegionLabel(x, y, extent, gc, "Bell Labs", 3,1, 1);
		drawRegionLabel(x, y, extent, gc, "Apple", 4,1, 1);
		drawRegionLabel(x, y, extent, gc, "MIT", 5,1, 1);
		drawRegionLabel(x, y, extent, gc, "DISA", 7,1, 1);
		
		drawRegionLabel(x, y, extent, gc, "BB&N", 0,2, 1);
		drawRegionLabel(x, y, extent, gc, "BB&N", 2,2, 1);
		drawRegionLabel(x, y, extent, gc, "DoD/Intel", 3,2, 1);
		drawRegionLabel(x, y, extent, gc, "DISA", 4,2, 2);
		drawRegionLabel(x, y, extent, gc, "Cable TV", 6,2, 1);
		drawRegionLabel(x, y, extent, gc, "UK/MoD", 7,2, 1);

		drawRegionLabel(x, y, extent, gc, "Army/AISC", 1,3, 1);
		drawRegionLabel(x, y, extent, gc, "IBM", 2,3, 1);
		drawRegionLabel(x, y, extent, gc, "VPNs", 3,3, 1);
		drawRegionLabel(x, y, extent, gc, "DSI", 5,3, 1);
		drawRegionLabel(x, y, extent, gc, "DISA", 6,3, 1);

		drawRegionLabel(x, y, extent, gc, "SITA", 1,4, 1);
		drawRegionLabel(x, y, extent, gc, "Merc", 2,4, 1);
		drawRegionLabel(x, y, extent, gc, "CAP/DEBIS/CCS", 3,4, 1);
		drawRegionLabel(x, y, extent, gc, "Norsk", 4,4, 1);
		drawRegionLabel(x, y, extent, gc, "Merit", 5,4, 1);

		drawRegionLabel(x, y, extent, gc, "USPS", 1,5, 1);
		drawRegionLabel(x, y, extent, gc, "Boeing", 2,5, 1);
		drawRegionLabel(x, y, extent, gc, "duPont", 3,5, 1);
		drawRegionLabel(x, y, extent, gc, "OLA", 4,5, 1);
		drawRegionLabel(x, y, extent, gc, "Haliburton", 5,5, 1);

		drawRegionLabel(x, y, extent, gc, "UK Social Security", 3,6, 1);
		drawRegionLabel(x, y, extent, gc, "BB&N", 4,6, 1);
		drawRegionLabel(x, y, extent, gc, "INTEROP", 5,6, 1);
		drawRegionLabel(x, y, extent, gc, "Eli Lily", 6,6, 1);
		drawRegionLabel(x, y, extent, gc, "AFRINIC", 7,6, 1);

		drawRegionLabel(x, y, extent, gc, "Prudential", 3,7, 1);
		drawRegionLabel(x, y, extent, gc, "Bell North", 4,7, 1);
		drawRegionLabel(x, y, extent, gc, "Ham Radio", 5,7, 1);
		drawRegionLabel(x, y, extent, gc, "Japan", 6,7, 1);

		drawRegionLabel(x, y, extent, gc, "Loopback", 7,8, 1);
	}
}