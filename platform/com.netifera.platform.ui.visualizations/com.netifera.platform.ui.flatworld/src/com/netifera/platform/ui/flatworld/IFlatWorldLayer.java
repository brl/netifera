package com.netifera.platform.ui.flatworld;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;

import com.netifera.platform.ui.flatworld.support.FloatRectangle;

public interface IFlatWorldLayer {
	void paint(FloatRectangle region, Rectangle rect, GC gc);
}
