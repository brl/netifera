package com.netifera.platform.ui.treemap.curves;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;

import com.netifera.platform.ui.treemap.IHilbertCurve;
import com.netifera.platform.util.addresses.inet.IPv4Netblock;


public abstract class AbstractXKCDHilbertCurve implements IHilbertCurve {

	private static int[] hilbertCurve = new int[256];
	
	static {
		int[] reverseMapping = {
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

		for (int i=0; i<=255; i++)
			hilbertCurve[reverseMapping[i]] = i;
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
		return "XKCD Map Of The Internet";
	}

	public int getIndex(IPv4Netblock netblock, IPv4Netblock subnetblock) {
		return hilbertCurve[subnetblock.getNetworkAddress().toBytes()[netblock.getCIDR()/8] & 0xff];
	}

	protected void drawRegion(int x, int y, int extent, GC gc, int[] coordinatesArray) {
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

	protected void drawRegionLabel(int x, int y, int extent, GC gc, String label, int i, int j, int w) {
		int fontSize = w*extent/16/label.length();
		if (fontSize <= 0 || fontSize >= 150)
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

	abstract protected void paintRegions(int x, int y, int extent, GC gc);
	
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

		paintRegions(x, y, extent, gc);
	}
}
