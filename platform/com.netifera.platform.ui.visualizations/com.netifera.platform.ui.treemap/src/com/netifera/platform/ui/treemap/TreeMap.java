package com.netifera.platform.ui.treemap;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Display;

import com.netifera.platform.api.model.IEntity;
import com.netifera.platform.util.addresses.inet.IPv4Address;
import com.netifera.platform.util.addresses.inet.IPv4Netblock;

public class TreeMap implements Iterable<IEntity> {
		
	final private TreeMap[] submaps = new TreeMap[256];
	final private IPv4Netblock netblock;
	final private Set<IEntity> entities = new HashSet<IEntity>();

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
	
/*	private double density() {
		if (netblock.getCIDR() == 0)
			return 0.0;
		if (netblock.getCIDR() == 32)
			return 1.0;
		
		return Math.min(1.0, (double)size()) / ((double)netblock.itemCount());
	}
*/
	private double temperature() {
		if (netblock.getCIDR() == 0)
			return 0.0;
		if (netblock.getCIDR() == 32)
			return 1.0;
		int n = size();
		if (n == 0)
			return 0.0;
		return Math.log((double)(n+1)) / Math.log((double)(netblock.itemCount()+1));
	}

	private double maximumTemperature() {
		if (netblock.getCIDR() == 0)
			return 0.0;
		if (netblock.getCIDR() == 32)
			return 1.0;
		if (netblock.getCIDR() == 24)
			return temperature();

		double temperature = temperature();
		for (TreeMap submap: submaps) {
			if (submap != null)
				temperature = Math.max(temperature, submap.maximumTemperature());
		}
		return temperature;
	}
	
/*	private double surfaceDensity() {
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
*/

	private int getIndex(IPv4Address address) {
		return address.toBytes()[netblock.getCIDR()/8] & 0xff;
	}
	
	public void add(IPv4Address address, IEntity entity) {
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

		submap.add(address, entity/*, color*/);
	}

	private Color getColorForTemperature(double temperature) {
		if (temperature <= 0.0)
			return Display.getDefault().getSystemColor(SWT.COLOR_MAGENTA); // shouldnt happen
		if (temperature < 0.2)
			return Display.getDefault().getSystemColor(SWT.COLOR_BLUE);
		if (temperature < 0.4)
			return Display.getDefault().getSystemColor(SWT.COLOR_CYAN);
		if (temperature < 0.6)
			return Display.getDefault().getSystemColor(SWT.COLOR_GREEN);
		if (temperature < 0.8)
			return Display.getDefault().getSystemColor(SWT.COLOR_YELLOW);
		return Display.getDefault().getSystemColor(SWT.COLOR_RED);
	}

	private boolean paintLabel(int x, int y, int extent, GC gc) {
		if (netblock.getCIDR() > 0) {
			String label = netblock.getCIDR() == 32 ? netblock.getNetworkAddress().toString() : netblock.toString();
			int fontSize = (extent-3)/label.length();
			if (fontSize <= 8)
				return false;
			if (fontSize > 18)
				fontSize = 18;
			Font font = new Font(Display.getDefault(),"Arial",fontSize,SWT.BOLD);
			if (font == null)
				return false;
			gc.setFont(font);
			gc.setAlpha(100);
			gc.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
			gc.drawString(label, x+2, y+2, true);
			font.dispose();
			return true;
		}
		return false;
	}
	
	public void paint(int x, int y, int extent, GC gc, IHilbertCurve curve) {
		if (extent <= 0)// dont draw at subpixel level, avoid unnecesary drawing of details that woudlnt be visible
			return;
		
		double temperature = maximumTemperature();
		if (temperature > 0.0) {
			gc.setAlpha((int)(255 / Math.sqrt(extent+1))); // as we zoom-in the outermost color fades out as the smaller detail is more visible
			gc.setBackground(getColorForTemperature(temperature));
			gc.fillRectangle(x, y, extent+1, extent+1);
		}
		
		// draw grid for /0. /8, /16 and /24 (all except individual addresses)
		if (netblock.getCIDR() < 32 && extent > 64) { // dont draw grid if the scale is too small
			gc.setAlpha(Math.min(extent, 256) / 24); // make the grid gradually appear as we zoom-in
			gc.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
			for (int i=1; i<16; i++) {
				int xi = x + (i*extent/16);
				gc.drawLine(xi, y, xi, y + extent);
				int yi = y + (i*extent/16);
				gc.drawLine(x, yi, x + extent, yi);
			}
		}

		boolean has0 = false;
		for (int i=0; i<256; i++) {
			TreeMap submap = submaps[i];
			if (submap != null) {
				int h = curve.getIndex(netblock, submap.netblock);
				int xi = h % 16;
				int yi = h / 16;
				int subX = x + (xi*extent/16);
				int subY = y + (yi*extent/16);
				int subExtent = extent/16;
				submap.paint(subX, subY, subExtent, gc, curve);
				if (h == 0)
					has0 = true;
			}
		}
		
		if (!has0 || extent < 16*(gc.stringExtent("0").y+3))
			paintLabel(x, y, extent, gc);
	}
}
