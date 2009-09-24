package com.netifera.platform.ui.treemap;

import org.eclipse.jface.viewers.IColorProvider;
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
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import com.netifera.platform.api.model.IEntity;
import com.netifera.platform.util.addresses.inet.IPv4Address;
import com.netifera.platform.util.addresses.inet.IPv4Netblock;

public class TreeMapWidget extends Canvas {

	private TreeMap treeMap;

	class TreeMapFrame {
		double scale = 1.0;
		int offsetX = 0, offsetY = 0;
	};
	
//	private List<TreeMapFrame> frameStack = new ArrayList<TreeMapFrame>();

	private TreeMapFrame frame = new TreeMapFrame();
	
	private IColorProvider colorProvider;

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
		
		Listener mouseListener = new Listener() {
			Integer clickX, clickY;
			double originalScale;
			int originalOffsetX, originalOffsetY;
			
			boolean panning = false;
			boolean zooming = false;
			
			public void handleEvent(Event event) {
				switch (event.type) {
				case SWT.MouseDoubleClick:
					frame.offsetX = 0;
					frame.offsetY = 0;
					frame.scale = 1.0;
					redraw();
					zooming = false;
					panning = false;
					break;
				case SWT.MouseDown:
					if (!panning && event.button == 1) {
						clickX = event.x;
						clickY = event.y;
						originalOffsetX = frame.offsetX;
						originalOffsetY = frame.offsetY;
						originalScale = frame.scale;
						panning = true;
					}
					if (!zooming && event.button == 3) {
						clickX = event.x;
						clickY = event.y;
						originalOffsetX = frame.offsetX;
						originalOffsetY = frame.offsetY;
						originalScale = frame.scale;
						zooming = true;
					}
					break;
				case SWT.MouseMove:
					if (panning) {
						frame.offsetX = originalOffsetX + (clickX - event.x);
						frame.offsetY = originalOffsetY + (clickY - event.y);
						redraw();
					}
					if (zooming) {
						frame.scale = originalScale * Math.pow(2.0, (clickY - event.y) / 10.0);
						frame.offsetX = (int)((clickX + originalOffsetX)*frame.scale/originalScale - clickX);
						frame.offsetY = (int)((clickY + originalOffsetY)*frame.scale/originalScale - clickY);
						redraw();
					}
					break;
				case SWT.MouseUp:
					clickX = null;
					clickY = null;
					panning = false;
					zooming = false;
				}
			}
		};
		
		addListener(SWT.MouseDoubleClick, mouseListener);
		addListener(SWT.MouseDown, mouseListener);
		addListener(SWT.MouseMove, mouseListener);
		addListener(SWT.MouseUp, mouseListener);
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
		
		treeMap.paint(rect.x - frame.offsetX, rect.y - frame.offsetY, (int) (Math.min(rect.width,rect.height) * frame.scale), gc);
	}
	
	public void add(IPv4Address address, IEntity entity) {
		treeMap.add(address, entity, colorProvider == null ? getForeground() : colorProvider.getForeground(entity));
		redraw();
	}
	
	public void reset() {
		initializeTreeMap();
		redraw();
	}
	
	public void setColorProvider(IColorProvider colorProvider) {
		this.colorProvider = colorProvider;
	}
}
