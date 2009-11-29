package com.netifera.platform.ui.treemap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistable;

import com.netifera.platform.api.model.IEntity;
import com.netifera.platform.api.model.layers.ISemanticLayer;
import com.netifera.platform.ui.internal.treemap.Activator;
import com.netifera.platform.ui.treemap.layers.GeolocationTreeMapLayer;
import com.netifera.platform.util.addresses.inet.IPv4Address;
import com.netifera.platform.util.addresses.inet.IPv4Netblock;

public class TreeMapControl extends Canvas implements IPersistable {

	private TreeMap treeMap;
	private ITreeMapLayer curve;
	
	private List<TreeMap> selection = new ArrayList<TreeMap>();

	class TreeMapFrame {
		double scale = 1.0;
		double offsetX = 0, offsetY = 0;
		
		void adjust() {
			Rectangle rect = getClientArea();
			int extent = (int) (Math.min(rect.width,rect.height) * scale);
			if (extent < rect.width) {
				offsetX = (extent - rect.width) / 2;
			} else {
				double w = extent - offsetX;
				if (w < rect.width)
					offsetX -= rect.width - w;
				if (offsetX < 0) offsetX = 0;
			}
			if (extent < rect.height) {
				offsetY = (extent - rect.height) / 2;
			} else {
				double h = extent - offsetY;
				if (h < rect.height)
					offsetY -= rect.height - h;
				if (offsetY < 0) offsetY = 0;
			}
		}
	};
	
	private TreeMapFrame frame = new TreeMapFrame();
	
	public TreeMapControl(Composite parent, int style) {
		super(parent, style);

		for (ISemanticLayer layer: Activator.getInstance().getModel().getSemanticLayers()) {
			if (layer instanceof GeolocationTreeMapLayer) {
				curve = (ITreeMapLayer) layer;
				break;
			}
		}
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

			boolean prePanning = false;
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
						prePanning = true;
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
					if (prePanning && Math.max(Math.abs(clickX - event.x),Math.abs(clickY - event.y)) > 3) {
						panning = true;
						prePanning = false;
					}
					if (panning ) {
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
/*					} else {
						TreeMap item = getItem(new Point(event.x, event.y));
						if (item != null) {
							selection = new TreeMap[] {item};
							redraw();
						}
*/					}
					break;
				case SWT.MouseUp:
					if (event.button == 1 && !panning) {
						TreeMap item = getItem(new Point(event.x, event.y));
						if (item != null) {
							if ((event.stateMask & SWT.CTRL) != 0) {
								if (selection.contains(item))
									selection.remove(item);
								else
									selection.add(item);
							} else {
								if (selection.size() == 1 && selection.contains(item)) {
									selection = new ArrayList<TreeMap>();
								} else {
									selection = new ArrayList<TreeMap>();
									selection.add(item);
								}
							}
							redraw();
						}
					}
					clickX = null;
					clickY = null;
					prePanning = false;
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
	
	public void saveState(IMemento memento) {
		memento.putFloat("frameOffsetX", (float)frame.offsetX);
		memento.putFloat("frameOffsetY", (float)frame.offsetY);
		memento.putFloat("frameScale", (float)frame.scale);
	}

	public void restoreState(IMemento memento) {
		frame.offsetX = memento.getFloat("frameOffsetX");
		frame.offsetY = memento.getFloat("frameOffsetY");
		frame.scale = memento.getFloat("frameScale");
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

		frame.adjust();
		
		int x = (int)(rect.x - frame.offsetX);
		int y = (int)(rect.y - frame.offsetY);
		int extent = (int) (Math.min(rect.width,rect.height) * frame.scale);
		treeMap.paint(x, y, extent, gc, curve, palette);

		for (Color color: palette)
			color.dispose();

		gc.setAlpha(255);
		gc.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		gc.setLineStyle(SWT.LINE_DASHDOT);
		gc.setLineWidth(1);
		gc.drawRectangle(x-1, y-1, extent+2, extent+2);

		if (selection.size() > 0) {
			gc.setLineStyle(SWT.LINE_SOLID);
			gc.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_LIST_SELECTION));
			for (TreeMap subtree: selection) {
				gc.drawRectangle(getItemBounds(subtree));
			}
		}
	}

	public ITreeMapLayer getLayer() {
		return curve;
	}

	public void setLayer(ITreeMapLayer layer) {
		this.curve = layer;
		redraw();
	}

	public void add(IPv4Address address, IEntity entity) {
		treeMap.add(address, entity);
		redraw();
	}
	
	public void reset() {
		selection = new ArrayList<TreeMap>();
		initializeTreeMap();
		redraw();
	}

	public TreeMap getItem(Point point) {
		Rectangle rect = getClientArea();
		double extent = Math.min(rect.width,rect.height) * frame.scale;
		
		// normalized coordinates in the interval [0,1)
		double x0 = (point.x - (rect.x - frame.offsetX)) / extent;
		double y0 = (point.y - (rect.y - frame.offsetY)) / extent;

		if (x0 > 1.0 || x0 < 0)
			return null;
		if (y0 > 1.0 || y0 < 0)
			return null;
		
		TreeMap tree = treeMap;
		while (extent > 128 && tree != null && tree.getNetblock().getCIDR() < 32) {
			x0 *= 16;
			y0 *= 16;
			int xi = (int) x0;
			int yi = (int) y0;
			x0 -= xi;
			y0 -= yi;
			IPv4Netblock subnet = curve.getSubNetblock(tree.getNetblock(), yi * 16 + xi);
			tree = tree.getSubTree(subnet);
			extent = extent / 16;
		}
		return tree;
	}
	
	public Rectangle getItemBounds(TreeMap subtree) {
		Rectangle rect = getClientArea();
		int x = rect.x - (int)frame.offsetX;
		int y = rect.y - (int)frame.offsetY;
		int extent = (int)(Math.min(rect.width,rect.height) * frame.scale);
		
		TreeMap tree = treeMap;
		while (!tree.getNetblock().equals(subtree.getNetblock())) {
			int h = curve.getIndex(tree.getNetblock(), subtree.getNetblock());
			int xi = h % 16;
			int yi = h / 16;
			x = x + (xi*extent/16);
			y = y + (yi*extent/16);
			extent = extent/16;
			tree = tree.getSubTree(subtree.getNetblock());
		}
		return new Rectangle(x, y, extent, extent);
	}

	public List<TreeMap> getSelection() {
		return Collections.unmodifiableList(selection);
	}
}
