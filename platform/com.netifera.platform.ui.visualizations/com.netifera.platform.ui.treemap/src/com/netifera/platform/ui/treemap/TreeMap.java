package com.netifera.platform.ui.treemap;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.graphics.GC;

import com.netifera.platform.api.model.IEntity;
import com.netifera.platform.util.addresses.inet.IPv4Address;
import com.netifera.platform.util.addresses.inet.IPv4Netblock;

public class TreeMap {
	final private TreeMap[] submaps = new TreeMap[256];
	final IPv4Netblock netblock;
	final List<IEntity> entities = new ArrayList<IEntity>();
	
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

	public void add(IPv4Address address, IEntity entity) {
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
		
		submap.add(address, entity);
	}

/*	public void paint(int x, int y, int extent, GC gc, Color[] palette) {
		paint(x,y,extent,gc,palette,1.0);
	}
	
	private void paint(int x, int y, int extent, GC gc, Color[] palette, double maximumDensity) {
		System.out.println("draw "+extent+" "+netblock.getCIDR()+" "+size()+" "+netblock.itemCount()+" "+density()+" "+surfaceDensity());

		if (density() > 0.0) {
			gc.setAlpha((int)(Math.sqrt(surfaceDensity() / maximumDensity)*255));
			gc.setBackground(palette[(int) (surfaceDensity()/maximumDensity*(palette.length-1))]);
			gc.fillRectangle(x, y, extent, extent+1);
		}
		
		maximumDensity = 0.0;
		for (TreeMap submap: submaps)
			if (submap != null)
				maximumDensity = Math.max(maximumDensity, submap.surfaceDensity());
		
		if (extent >= 0) {
			for (int i=0; i<16; i++) {
				for (int j=0; j<16; j++) {
					TreeMap submap = submaps[i*16+j];
					if (submap != null) {
						submap.paint(x + (i*extent/16), y + (j*extent/16), extent/16, gc, palette, maximumDensity);
					}
				}
			}
		}
	}
*/
	
	public void paint(int x, int y, int extent, GC gc) {
		System.out.println("draw "+extent+" "+netblock.getCIDR()+" "+size()+" "+netblock.itemCount()+" "+surfaceDensity());

		if (density() > 0.0) {
			gc.setAlpha((int)(Math.sqrt(surfaceDensity())*255));
//			gc.setBackground(palette[(int) (surfaceDensity()*(palette.length-1))]);
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
