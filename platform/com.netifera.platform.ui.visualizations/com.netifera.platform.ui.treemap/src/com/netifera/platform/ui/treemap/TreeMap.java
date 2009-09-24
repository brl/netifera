package com.netifera.platform.ui.treemap;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;

import com.netifera.platform.api.model.IEntity;
import com.netifera.platform.util.addresses.inet.IPv4Address;
import com.netifera.platform.util.addresses.inet.IPv4Netblock;

public class TreeMap implements Iterable<IEntity> {
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

	public void add(IPv4Address address, IEntity entity, Color color) {
		if (this.color == null)
			this.color = color;
		
		if (!netblock.contains(address))
			return;

		if (netblock.getCIDR() == 32) {
			entities.add(entity);
			return;
		}
		
		int index = address.toBytes()[netblock.getCIDR()/8] & 0xff;
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
		
		if (extent >= 0) {
			for (int i=0; i<16; i++) {
				for (int j=0; j<16; j++) {
					TreeMap submap = submaps[i*16+j];
					if (submap != null) {
						submap.paint(x + (i*extent/16), y + (j*extent/16), extent/16, gc);
					}
				}
			}
		}
	}
}
