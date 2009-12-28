package com.netifera.platform.ui.flatworld.layers;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

import com.netifera.platform.ui.flatworld.IFlatWorldLayer;
import com.netifera.platform.ui.flatworld.support.FloatPoint;
import com.netifera.platform.ui.flatworld.support.FloatRectangle;

public class FocusFlatWorldLayer implements IFlatWorldLayer {

	private FloatPoint location = null;

	public synchronized void unsetFocus() {
		location = null;
	}

	public synchronized void setFocus(double latitude, double longitude) {
		location = new FloatPoint((float)longitude, (float)latitude);
	}

	public synchronized void paint(final FloatRectangle region, final Rectangle rect, final GC gc) {
		if (!isActive())
			return;
		
		final long now = System.currentTimeMillis();
		int x = (int) ((location.x-region.x)/region.width*rect.width + rect.x);
		int y = (int) ((region.y-location.y)/region.height*rect.height + rect.y + rect.height);

		int radius = (int)((now % 1024) * 64 / 1024);
		radius = Math.abs(radius - 32);
		gc.setLineWidth(1);
		gc.setLineStyle(SWT.LINE_SOLID);
		gc.setAlpha(255);
		gc.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_LIST_SELECTION));
		int diameter = radius * 2;
		gc.drawOval(x-radius, y-radius, diameter, diameter);
	}
	
	public synchronized boolean isActive() {
		return location != null;
	}
}
