package com.netifera.platform.ui.heatmap;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

import com.netifera.platform.api.model.IEntity;
import com.netifera.platform.util.addresses.inet.IPv4Address;
import com.netifera.platform.util.addresses.inet.IPv4Netblock;

public class HeatMap implements Iterable<IEntity> {
	final private HeatMap[] children = new HeatMap[256];
	final private IPv4Netblock netblock;
	final private Set<IEntity> entities = new HashSet<IEntity>(1);

	class HeatMapIterator implements Iterator<IEntity> {
		final private Iterator<HeatMap> childrenIterator;
		private Iterator<IEntity> entitiesIterator;
		
		HeatMapIterator(HeatMap heatMap) {
			entitiesIterator = heatMap.entities.iterator();
			List<HeatMap> nonNullChildren = new ArrayList<HeatMap>();
			for (HeatMap child: heatMap.children) {
				if (child != null)
					nonNullChildren.add(child);
			}
			childrenIterator = nonNullChildren.iterator();
		}
		
		public boolean hasNext() {
			return entitiesIterator.hasNext() || childrenIterator.hasNext();
		}

		public IEntity next() {
			if (!entitiesIterator.hasNext())
				entitiesIterator = childrenIterator.next().iterator();
			return entitiesIterator.next();
		}

		public void remove() {
		}
	}
	
	public HeatMap(IPv4Netblock netblock) {
		this.netblock = netblock;
	}

	public IPv4Netblock getNetblock() {
		return netblock;
	}
	
	public int size() {
		int answer = entities.size();
		for (HeatMap child: children) {
			if (child != null)
				answer += child.size();
		}
		return answer;
	}

	public boolean equals(Object o) {
		return (o instanceof HeatMap) && netblock.equals(((HeatMap)o).netblock);
	}
	
	public int hashCode() {
		return netblock.hashCode();
	}
	
	public Iterator<IEntity> iterator() {
		return new HeatMapIterator(this);
	}
	
	public HeatMap getChild(IPv4Netblock subnet) {
		if (!netblock.contains(subnet.getNetworkAddress()))
			return null;
		HeatMap child = children[subnet.getNetworkAddress().toBytes()[netblock.getCIDR()/8] & 0xff];
		return child != null ? child : new HeatMap(subnet);
	}

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
		HeatMap child = children[index];
		
		if (child == null) {
			child = new HeatMap(new IPv4Netblock(address, netblock.getCIDR()+8));
			children[index] = child;
		}

		child.add(address, entity/*, color*/);
	}

	private double temperature() {
		if (netblock.getCIDR() == 0)
			return 0.0;
		if (netblock.getCIDR() == 32)
			return 1.0;
		int n = size();
		if (n == 0)
			return 0.0;
		return Math.log((double)(n+1)) / Math.log((double)(netblock.size()+1));
	}

	private double maximumTemperature() {
		if (netblock.getCIDR() == 0)
			return 0.0;
		if (netblock.getCIDR() == 32)
			return 1.0;
		if (netblock.getCIDR() == 24)
			return temperature();

		double temperature = temperature();
		for (HeatMap child: children) {
			if (child != null)
				temperature = Math.max(temperature, child.maximumTemperature());
		}
		return temperature;
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
	
	public void paint(int x, int y, int extent, GC gc, IHeatMapLayer curve, Color palette[]) {
		double temperature = maximumTemperature();
		if (temperature > 0.0) {
			Color color = palette[(int)(temperature*(palette.length-1))];
			float alpha = Math.min(extent, 255.0f);
			alpha = (255.0f - alpha) / 255.0f;
			alpha *= alpha * alpha;
			gc.setAlpha((int)(255.0f*alpha)); // as we zoom-in the outermost color fades out as the smaller detail is more visible
			if (extent > 0) {
				gc.setBackground(color);
				gc.fillRectangle(x, y, extent+1, extent+1);
			} else {
//				gc.setAlpha(128);
				gc.setForeground(color);
				gc.drawPoint(x+1, y+1);
			}
		}

		if (extent <= 0)// dont draw at subpixel level, avoid unnecesary drawing of details that wouldnt be visible
			return;

		// draw grid and curve regions for /0. /8, /16 and /24 (all except individual addresses)
		if (netblock.getCIDR() < 32 && extent > 64) { // dont draw grid if the scale is too small
			gc.setAlpha(Math.min(extent, 255) / 24); // make the grid gradually appear as we zoom-in
			gc.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
			for (int i=1; i<16; i++) {
				int xi = x + (i*extent/16);
				gc.drawLine(xi, y, xi, y + extent);
				int yi = y + (i*extent/16);
				gc.drawLine(x, yi, x + extent, yi);
			}
			
			curve.paint(x, y, extent, gc, netblock); // draw curve regions
		}

		for (int i=0; i<256; i++) {
			HeatMap child = children[i];
			if (child != null) {
				int h = curve.getIndex(netblock, child.netblock);
				int xi = h % 16;
				int yi = h / 16;
				int subX = x + (xi*extent/16);
				int subY = y + (yi*extent/16);
				int subExtent = extent/16;
				Rectangle subRect = new Rectangle(subX, subY, subExtent, subExtent);
				if (subRect.intersects(gc.getClipping())) {
					child.paint(subX, subY, subExtent, gc, curve, palette);
				}
			}
		}
		
		paintLabel(x, y, extent, gc);
	}
}
