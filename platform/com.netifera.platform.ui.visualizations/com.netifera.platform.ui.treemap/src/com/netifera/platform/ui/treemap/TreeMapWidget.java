package com.netifera.platform.ui.treemap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import com.netifera.platform.api.model.IEntity;
import com.netifera.platform.util.addresses.inet.IPv4Address;
import com.netifera.platform.util.addresses.inet.IPv4Netblock;

public class TreeMapWidget extends Canvas {

	private TreeMap treeMap;

	public TreeMapWidget(Composite parent, int style) {
		super(parent, style);

		initializeTreeMap();

		setBackground(Display.getDefault().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		setForeground(Display.getDefault().getSystemColor(SWT.COLOR_GREEN));
		
		addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				redraw();
			}
		});
		
		addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {
				paint(e);
			}
		});
	}

	private void initializeTreeMap() {
		treeMap = new TreeMap((IPv4Netblock)IPv4Netblock.fromString("0.0.0.0/0"));
	}
	
	private void paint(PaintEvent event) {
		GC gc = event.gc;
		
		gc.setAntialias(SWT.ON);
		
		gc.setLineWidth(1);
		Rectangle rect = getClientArea();
		gc.setClipping(rect);
		gc.setForeground(getForeground());
		gc.setBackground(getForeground());
		
//		Color[] palette = {Display.getDefault().getSystemColor(SWT.COLOR_WHITE), Display.getDefault().getSystemColor(SWT.COLOR_GREEN), Display.getDefault().getSystemColor(SWT.COLOR_YELLOW), Display.getDefault().getSystemColor(SWT.COLOR_RED)};
		
		treeMap.paint(rect.x, rect.y, rect.width, gc);
	}
	
	public void add(IPv4Address address, IEntity entity) {
		treeMap.add(address, entity);
		redraw();
	}
	
	public void reset() {
		initializeTreeMap();
		redraw();
	}
}
