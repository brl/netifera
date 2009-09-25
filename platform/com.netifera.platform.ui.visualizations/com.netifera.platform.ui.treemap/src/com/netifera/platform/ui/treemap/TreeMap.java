package com.netifera.platform.ui.treemap;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;

import com.netifera.platform.api.model.IEntity;
import com.netifera.platform.util.addresses.inet.IPv4Address;
import com.netifera.platform.util.addresses.inet.IPv4Netblock;

public class TreeMap implements Iterable<IEntity> {
	
	private static int[] hilbertCurve = new int[256];
	
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
	}
	
	final private TreeMap[] submaps = new TreeMap[256];
	final private IPv4Netblock netblock;
	final private Set<IEntity> entities = new HashSet<IEntity>();
	private Color color;

	class TreeMapIterator implements Iterator<IEntity> {
		final private Iterator<TreeMap> treeMapsIterator;
		private Iterator<IEntity> entitiesIterator;
		
		TreeMapIterator(TreeMap treeMap) {
			entitiesIterator = treeMap.entities.iterator();
			List<TreeMap> treeMaps = new ArrayList<TreeMap>();
			for (TreeMap submap: treeMap.submaps) {
				if (submap != null)
					treeMaps.add(submap);
			}
			treeMapsIterator = treeMaps.iterator();
		}
		
		public boolean hasNext() {
			return entitiesIterator.hasNext() || treeMapsIterator.hasNext();
		}

		public IEntity next() {
			if (!entitiesIterator.hasNext())
				entitiesIterator = treeMapsIterator.next().iterator();
			return entitiesIterator.next();
		}

		public void remove() {
		}
	}
	
	public TreeMap(IPv4Netblock netblock) {
		this.netblock = netblock;
	}

	public int size() {
		int answer = entities.size();
		for (TreeMap submap: submaps) {
			if (submap != null)
				answer += submap.size();
		}
		return answer;
	}

	public Iterator<IEntity> iterator() {
		return new TreeMapIterator(this);
	}
	
	private double density() {
		if (netblock.getCIDR() == 0)
			return 0.0;
		if (netblock.getCIDR() == 32)
			return 1.0;
		
		return Math.min(1.0, (double)size()) / ((double)netblock.itemCount());
	}

	private double surfaceDensity() {
		if (netblock.getCIDR() == 0)
			return 0.0;
		if (netblock.getCIDR() == 32)
			return 1.0;
		
		int count = 0;
		for (TreeMap submap: submaps)
			if (submap != null)
				count += 1;
		return ((double) count) / 256.0;
	}

	private int getIndex(IPv4Address address) {
		return hilbertCurve[address.toBytes()[netblock.getCIDR()/8] & 0xff];
	}
	
	public void add(IPv4Address address, IEntity entity, Color color) {
		if (this.color == null)
			this.color = color;
		
		if (!netblock.contains(address))
			return;

		if (netblock.getCIDR() == 32) {
			entities.add(entity);
			return;
		}
		
		int index = getIndex(address);
		TreeMap submap = submaps[index];
			
		if (submap == null) {
			submap = new TreeMap(new IPv4Netblock(address, netblock.getCIDR()+8));
			submaps[index] = submap;
		}

		submap.add(address, entity, color);
	}
	
	public void paint(int x, int y, int extent, GC gc) {
		if (density() > 0.0) {
			gc.setAlpha((int)(32+Math.sqrt(surfaceDensity())*(255-32)));
			gc.setBackground(color);
			gc.fillRectangle(x, y, extent, extent+1);
		}

		if (netblock.getCIDR() > 0) {
			String label = ""+(netblock.getNetworkAddress().toBytes()[netblock.getCIDR()/8-1] & 0xff);
			Point labelExtent = gc.stringExtent(label);
			if (extent >= Math.max(labelExtent.x, labelExtent.y)+3) {
				gc.setAlpha(100);
				gc.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
				gc.drawString(label, x+2, y+2, true);
			}
		} else {
			//TODO draw top level labels
		}
		
		if (extent > 0) {//XXX
			for (int i=0; i<16; i++) {
				for (int j=0; j<16; j++) {
					TreeMap submap = submaps[j*16+i];
					if (submap != null) {
						submap.paint(x + (i*extent/16), y + (j*extent/16), extent/16, gc);
					}
				}
			}
		}
	}
}
