package com.netifera.platform.ui.flatworld.layers;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

import com.netifera.platform.ui.flatworld.IFlatWorldLayer;
import com.netifera.platform.ui.flatworld.quadtrees.IQuadTreeElementsVisitor;
import com.netifera.platform.ui.flatworld.quadtrees.QuadTree;
import com.netifera.platform.ui.flatworld.support.FloatPoint;
import com.netifera.platform.ui.flatworld.support.FloatRectangle;

public class LabelsFlatWorldLayer implements IFlatWorldLayer {

	private QuadTree<String> labels = new QuadTree<String>(new FloatRectangle(-180,-90,360,180));

	public void addLabel(double latitude, double longitude, String label) {
		if (label == null) return;
		labels.put(new FloatPoint((float)longitude, (float)latitude), label);
	}

	public void paint(final FloatRectangle region, final Rectangle rect, final GC gc) {
		labels.visit(region, new IQuadTreeElementsVisitor<String>() {
			public void visit(QuadTree<String> tree, FloatPoint location, String label) {
				int x = (int) ((location.x-region.x)/region.width*rect.width + rect.x);
				int y = (int) ((region.y-location.y)/region.height*rect.height + rect.y + rect.height);
				
				int w = (int)(rect.width/(rect.width/tree.getBounds().width));
				int h = (int)(rect.height/(region.height/tree.getBounds().height));
				int fontSize = (w+h)/2/label.length();
				if (fontSize <= 0) fontSize = 1;
				if (fontSize >= 48) fontSize = 48;
				Font font = new Font(Display.getDefault(),"Arial",fontSize,SWT.BOLD);
				gc.setAlpha(128-fontSize);
				gc.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
				gc.setFont(font);

				gc.drawString(label, x, y, true);
				font.dispose();

				gc.setAlpha(64);
				gc.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_RED));
//				int w = label.length()*2;
				int d = Math.max(8, fontSize);
				gc.fillOval(x-d, y-d, d*2, d*2);
			}
		});
	}
}
