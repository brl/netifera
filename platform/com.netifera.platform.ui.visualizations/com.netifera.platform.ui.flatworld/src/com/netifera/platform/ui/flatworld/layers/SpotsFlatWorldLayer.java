package com.netifera.platform.ui.flatworld.layers;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

import com.netifera.platform.ui.flatworld.IFlatWorldLayer;
import com.netifera.platform.ui.flatworld.quadtrees.IQuadTreeElementsVisitor;
import com.netifera.platform.ui.flatworld.quadtrees.QuadTree;
import com.netifera.platform.ui.flatworld.support.FloatPoint;
import com.netifera.platform.ui.flatworld.support.FloatRectangle;

public class SpotsFlatWorldLayer<E> implements IFlatWorldLayer {

	private QuadTree<Spot> spots = new QuadTree<Spot>(new FloatRectangle(-180,-90,360,180));
	private int maxCount = 0;
	
	class Spot {
		String label;
		Set<E> elements;
	}

	public synchronized void addSpot(double latitude, double longitude, String label, E element) {
		//HACK round to 0.2 precision because the geoip database has repeated entries for cities with different coordinates (maybe because the postal codes are different)
		latitude = Math.floor(latitude * 5.0) / 5.0;
		longitude = Math.floor(longitude * 5.0) / 5.0;
		
		FloatPoint location = new FloatPoint((float)longitude, (float)latitude);
		Spot spot = spots.get(location);
		if (spot != null) {
			spot.elements.add(element);
		} else {
			spot = new Spot();
			spot.elements = new HashSet<E>(1);
			spot.elements.add(element);
			spots.put(location, spot);
		}
		if (label != null) {
			spot.label = label;
		}
		maxCount = Math.max(maxCount, spot.elements.size());
	}

	public synchronized void paint(final FloatRectangle region, final Rectangle rect, final GC gc) {
		if (maxCount == 0)
			return;

		final Color[] palette = new Color[16];
		for (int i=0; i<palette.length; i++) {
/*			float hue = 52.0f * i / (palette.length-1);
			float saturation = (30.0f * (palette.length-1-i) / (palette.length-1) + 50.0f) / 100.0f;
			float value = (10.0f * (palette.length-1-i) / (palette.length-1) + 84.0f) / 100.0f;
			palette[palette.length-1-i] = new Color(Display.getCurrent(), new RGB(hue, saturation, value));
*/			float hue = 24.0f * i / (palette.length-1);
			float saturation = 1.0f;
			float value = 1.0f;
			palette[palette.length-1-i] = new Color(Display.getCurrent(), new RGB(hue, saturation, value));
//			gc.setBackground(palette[palette.length-1-i]);
//			gc.fillRectangle(rect.x + i*rect.width/palette.length, rect.y, rect.width/palette.length, 100);
		}

		final float logMaxCount = (float)Math.log(maxCount);
		final float ws = spots.getBounds().width / region.width / logMaxCount;
		final float hs = spots.getBounds().height / region.height / logMaxCount;
		final float alphaMultiplier = Math.min(rect.width,rect.height) / 3.0f;
		spots.visit(region, new IQuadTreeElementsVisitor<Spot>() {
			public void visit(QuadTree<Spot> tree, FloatPoint location, Spot spot) {
				int x = (int) ((location.x-region.x)/region.width*rect.width + rect.x);
				int y = (int) ((region.y-location.y)/region.height*rect.height + rect.y + rect.height);
				
				float logCount = (float)Math.log(spot.elements.size());
				int w = (int)(rect.width/4*ws*logCount);
				int h = (int)(rect.height/4*hs*logCount);
				
				int radius = Math.max(8, Math.min(w, h));
				
				float alphaScale = Math.min(1.0f, alphaMultiplier/radius);
				
				gc.setAlpha((int)(64.0f * alphaScale));
				gc.setBackground(palette[(int)((palette.length-1)*logCount/logMaxCount)]);
				gc.fillOval(x-radius, y-radius, radius*2, radius*2);
				
				int fw = (int)(rect.width/(region.width/tree.getBounds().width)/2.0f);
				int fh = (int)(rect.height/(region.height/tree.getBounds().height)/2.0f);
				int fontSize = Math.max(radius*4, Math.min(fw,fh))/spot.label.length();
				if (fontSize <= 0) fontSize = 1;
				if (fontSize >= 48) fontSize = 48;
				Font font = new Font(Display.getDefault(),"Arial",fontSize,SWT.BOLD);
				if (fontSize >= 10)
					gc.setAlpha((int)(128.0 * Math.sqrt(alphaScale))); // big labels become more translucent when they are too big
				else
					gc.setAlpha((int)(128.0 * Math.sqrt(alphaScale) * fontSize / 10.0)); // small labels become more translucent whne they become smaller
				gc.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
				gc.setFont(font);

//				Point extent = gc.stringExtent(spot.label);
				gc.drawString(spot.label, x, y, true);
				font.dispose();
			}
		});
		
		for (Color color: palette)
			color.dispose();
	}
}
