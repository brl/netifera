package com.netifera.platform.ui.treemap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import com.netifera.platform.api.model.IEntity;
import com.netifera.platform.ui.treemap.curves.RegistriesHilbertCurve;
import com.netifera.platform.util.addresses.inet.IPv4Address;
import com.netifera.platform.util.addresses.inet.IPv4Netblock;

public class TreeMapControl extends Canvas {

	private TreeMap treeMap;
	private IHilbertCurve curve = new RegistriesHilbertCurve();

	class TreeMapFrame {
		double scale = 1.0;
		double offsetX = 0, offsetY = 0;
		
		void adjust() {
			Rectangle rect = getClientArea();
			int extent = (int) (Math.min(rect.width,rect.height) * scale);
			double w = extent - offsetX;
			double h = extent - offsetY;
			if (w < rect.width)
				offsetX -= rect.width - w;
			if (h < rect.height)
				offsetY -= rect.height - h;
			if (offsetX < 0) offsetX = 0;
			if (offsetY < 0) offsetY = 0;
		}
	};
	
//	private List<TreeMapFrame> frameStack = new ArrayList<TreeMapFrame>();

	private TreeMapFrame frame = new TreeMapFrame();
	
	public TreeMapControl(Composite parent, int style) {
		super(parent, style);

		initializeTreeMap();

		setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
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
			double originalScale, originalOffsetX, originalOffsetY;
			
			boolean panning = false;
			boolean zooming = false;
			
			public void handleEvent(Event event) {
				switch (event.type) {
				case SWT.MouseDoubleClick:
					if (event.button == 2 || event.button == 3) {
						frame.offsetX = 0;
						frame.offsetY = 0;
						frame.scale = 1.0;
						redraw();
						zooming = false;
						panning = false;
					}
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
						frame.adjust();
						redraw();
					}
					if (zooming) {
						frame.scale = Math.max(1.0, originalScale * Math.pow(2.0, (clickY - event.y) / 10.0));
						frame.offsetX = (clickX + originalOffsetX)*frame.scale/originalScale - clickX;
						frame.offsetY = (clickY + originalOffsetY)*frame.scale/originalScale - clickY;
						frame.adjust();
						redraw();
					}
					break;
				case SWT.MouseUp:
					clickX = null;
					clickY = null;
					panning = false;
					zooming = false;
					break;
				case SWT.MouseWheel:
					originalScale = frame.scale;
					frame.scale = Math.max(1.0, originalScale * Math.pow(2.0, event.count / 10.0));
					frame.offsetX = (event.x + frame.offsetX)*frame.scale/originalScale - event.x;
					frame.offsetY = (event.y + frame.offsetY)*frame.scale/originalScale - event.y;
					frame.adjust();
					redraw();
				}
			}
		};
		
		addListener(SWT.MouseDoubleClick, mouseListener);
		addListener(SWT.MouseDown, mouseListener);
		addListener(SWT.MouseMove, mouseListener);
		addListener(SWT.MouseUp, mouseListener);
		addListener(SWT.MouseWheel, mouseListener);
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
		
		Color[] palette = new Color[32];
		for (int i=0; i<palette.length; i++) {
			float hue = 270.0f * i / (palette.length-1);
			palette[palette.length-1-i] = new Color(Display.getDefault(), new RGB(hue, 1.0f, 1.0f));
//			gc.setBackground(palette[i]);
//			gc.fillRectangle(rect.x + i*rect.width/palette.length, rect.y, rect.width/palette.length, 100);
		}

		treeMap.paint((int)(rect.x - frame.offsetX), (int)(rect.y - frame.offsetY), (int) (Math.min(rect.width,rect.height) * frame.scale), gc, curve, palette);

		for (Color color: palette)
			color.dispose();
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
