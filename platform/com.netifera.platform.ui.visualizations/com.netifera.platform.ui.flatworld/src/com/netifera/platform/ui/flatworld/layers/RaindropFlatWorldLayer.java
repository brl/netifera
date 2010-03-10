package com.netifera.platform.ui.flatworld.layers;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;

import com.netifera.platform.ui.flatworld.IFlatWorldLayer;
import com.netifera.platform.ui.flatworld.quadtrees.IQuadTreeElementsVisitor;
import com.netifera.platform.ui.flatworld.quadtrees.QuadTree;
import com.netifera.platform.ui.flatworld.support.FloatPoint;
import com.netifera.platform.ui.flatworld.support.FloatRectangle;

public class RaindropFlatWorldLayer implements IFlatWorldLayer {

	private QuadTree<Raindrop> raindrops = new QuadTree<Raindrop>(new FloatRectangle(-180,-90,360,180));
	private boolean active = false;
	
	private class Raindrop {
		Color color;
		long time;
	}
	
	public synchronized void addRaindrop(double latitude, double longitude, Color color) {
		Raindrop raindrop = new Raindrop();
		raindrop.color = color;
		raindrop.time = System.currentTimeMillis();
		raindrops.put(new FloatPoint((float)longitude, (float)latitude), raindrop);
		active = true;
	}

	public synchronized void paint(final FloatRectangle region, final Rectangle rect, final GC gc) {
		if (!isActive())
			return;

		active = false;
		final long now = System.currentTimeMillis();
		raindrops.visit(region, new IQuadTreeElementsVisitor<Raindrop>() {
			public void visit(QuadTree<Raindrop> tree, FloatPoint location, Raindrop raindrop) {
				int x = (int) ((location.x-region.x)/region.width*rect.width + rect.x);
				int y = (int) ((region.y-location.y)/region.height*rect.height + rect.y + rect.height);

//				int w = (int)(rect.width/(rect.width/tree.getBounds().width));
//				int h = (int)(rect.height/(region.height/tree.getBounds().height));

				int alpha = 64 - (int) ((now-raindrop.time)*(now-raindrop.time)*64/(1024*1024));
				int radius = (int) ((now-raindrop.time)*64/1024);
				if (alpha <= 0)
					return; //FIXME remove draindrop
				active = true;
				gc.setAlpha(alpha);
				gc.setBackground(raindrop.color);
				double scale = 1.0;//Math.min(128.0, Math.max(24.0, Math.max(w, h))) / 128.0;
				int diameter = (int)(radius * scale * 2);
				radius = (int)(radius * scale);
				gc.fillOval(x-radius, y-radius, diameter, diameter);
			}
		});
	}
	
	public synchronized boolean isActive() {
		return active;
	}
}
