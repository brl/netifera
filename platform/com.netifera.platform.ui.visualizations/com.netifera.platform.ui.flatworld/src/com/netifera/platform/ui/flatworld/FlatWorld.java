package com.netifera.platform.ui.flatworld;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import com.netifera.platform.ui.flatworld.quadtrees.IQuadTreeElementsVisitor;
import com.netifera.platform.ui.flatworld.quadtrees.QuadTree;
import com.netifera.platform.ui.flatworld.support.FloatPoint;
import com.netifera.platform.ui.flatworld.support.FloatRectangle;
import com.netifera.platform.ui.internal.flatworld.Activator;

public class FlatWorld extends Canvas {

	private Image texture;
	private QuadTree<String> labels;
	
	class Frame {
		double scale = 1.0;
		int offsetX = 0, offsetY = 0;
		
		void adjust() {
			Rectangle textureBounds = texture.getBounds();
			int srcX = textureBounds.x + frame.offsetX;
			int srcY = textureBounds.y + frame.offsetY;
			int srcWidth = (int)(textureBounds.width / frame.scale);
			int srcHeight = (int)(textureBounds.height / frame.scale);
			if (srcX + srcWidth > textureBounds.x + textureBounds.width)
				offsetX -= srcX + srcWidth - (textureBounds.x + textureBounds.width);
			if (srcY + srcHeight > textureBounds.y + textureBounds.height)
				offsetY -= srcY + srcHeight - (textureBounds.y + textureBounds.height);
			if (offsetX < 0) offsetX = 0;
			if (offsetY < 0) offsetY = 0;
		}
	};
	
//	private List<TreeMapFrame> frameStack = new ArrayList<TreeMapFrame>();

	private Frame frame = new Frame();
	
	public FlatWorld(Composite parent, int style) {
		super(parent, style);

		initializeTexture();
		initializeLayers();

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
						Rectangle rect = getClientArea();
						Rectangle textureBounds = texture.getBounds();
						frame.offsetX = originalOffsetX + (int)((clickX - event.x)/frame.scale*textureBounds.width/rect.width);
						frame.offsetY = originalOffsetY + (int)((clickY - event.y)/frame.scale*textureBounds.height/rect.height);
						frame.adjust();
						redraw();
					}
					if (zooming) {
						frame.scale = Math.max(1.0, originalScale * Math.pow(2.0, (clickY - event.y) / 10.0));
						double lambda = 1.0/originalScale - 1.0/frame.scale;
						Rectangle rect = getClientArea();
						Rectangle textureBounds = texture.getBounds();
						frame.offsetX = originalOffsetX + (int)(clickX*textureBounds.width/rect.width*lambda);
						frame.offsetY = originalOffsetY + (int)(clickY*textureBounds.height/rect.height*lambda);
						frame.adjust();
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

	private void initializeTexture() {
		ImageDescriptor imageDescriptor = AbstractUIPlugin.imageDescriptorFromPlugin(Activator.PLUGIN_ID, "textures/TrueMarble.16km.2700x1350.png");
//		ImageDescriptor imageDescriptor = AbstractUIPlugin.imageDescriptorFromPlugin(Activator.PLUGIN_ID, "textures/TrueMarble.32km.1350x675.png");
		texture = imageDescriptor.createImage();
	}
	
	private void paint(PaintEvent event) {
		final GC gc = event.gc;

		gc.setAntialias(SWT.ON);
		gc.setInterpolation(SWT.HIGH);
		gc.setAdvanced(false);

		final Rectangle rect = getClientArea();
		gc.setClipping(rect);
		
//		gc.setLineWidth(1);
//		gc.setForeground(getForeground());
//		gc.setBackground(getForeground());

		Rectangle textureBounds = texture.getBounds();
		int srcX = textureBounds.x + frame.offsetX;
		int srcY = textureBounds.y + frame.offsetY;
		int srcWidth = (int)(textureBounds.width / frame.scale);
		int srcHeight = (int)(textureBounds.height / frame.scale);
		gc.drawImage(texture, srcX, srcY, srcWidth, srcHeight, rect.x, rect.y, rect.width, rect.height);


		final FloatRectangle region = getGeographicalRegionFromTextureRegion(srcX,srcY,srcWidth,srcHeight);
		labels.visit(region, new IQuadTreeElementsVisitor<String>() {
			public void visit(QuadTree<String> tree, FloatPoint location, String label) {
				Point screenCoordinates = getScreenCoordinatesFromLocation(location);
				gc.setAlpha(128);
				gc.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
				
				int w = (int)(rect.width/(region.width/tree.getBounds().width));
				int h = (int)(rect.height/(region.height/tree.getBounds().height));
				int fontSize = (w+h)/2/label.length();
				if (fontSize <= 0) fontSize = 1;
				if (fontSize >= 48) fontSize = 48;
				Font font = new Font(Display.getDefault(),"Arial",fontSize,SWT.BOLD);
				gc.setAlpha(128-fontSize);
				gc.setFont(font);

				gc.drawString(label, screenCoordinates.x, screenCoordinates.y, true);
				font.dispose();
				
				gc.setAlpha(64);
				gc.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_RED));
//				int w = label.length()*2;
				int d = Math.min(w,h) / 2;
				gc.fillOval(screenCoordinates.x-d, screenCoordinates.y-d, d*2, d*2);
			}
		});

/*		labels.visit(region, new IQuadTreeVisitor<String>() {
			public boolean visit(QuadTree<String> tree) {
				if (tree.size() > 0) {
					gc.setAlpha(64/tree.size());
					gc.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_RED));
					Rectangle rect = getScreenRegionFromGeographicalRegion(tree.getBounds());
					int w = Math.min(rect.width, rect.height);
					gc.fillOval(rect.x+rect.width/2-w/2, rect.y+rect.height/2-w/2, w, w);
				}
				return true;
			}
		});
*/	}
	
	private FloatRectangle getGeographicalRegionFromTextureRegion(int x, int y, int width, int height) {
		Rectangle textureBounds = texture.getBounds();
		return new FloatRectangle((x-textureBounds.x)*360/textureBounds.width-180,(y-textureBounds.y)*-180/textureBounds.height+90 - 180*height/textureBounds.height,360*width/textureBounds.width,180*height/textureBounds.height);
	}

	private Point getScreenCoordinatesFromLocation(FloatPoint location) {
		Rectangle textureBounds = texture.getBounds();
		Rectangle rect = getClientArea();
		int x = (int) (((location.x+180.0)/360.0*textureBounds.width - frame.offsetX) * frame.scale / textureBounds.width * rect.width) + rect.x;
		int y = (int) (((90.0-location.y)/180.0*textureBounds.height - frame.offsetY) * frame.scale / textureBounds.height * rect.height) + rect.y;
		return new Point(x,y);
	}

	private Rectangle getScreenRegionFromGeographicalRegion(FloatRectangle region) {
		Point bottomLeft = getScreenCoordinatesFromLocation(region.topLeft());
		Point topRight = getScreenCoordinatesFromLocation(region.bottomRight());
		return new Rectangle(bottomLeft.x, topRight.y, topRight.x-bottomLeft.x, bottomLeft.y-topRight.y);
	}

	public void initializeLayers() {
		labels = new QuadTree<String>(new FloatRectangle(-180,-90,360,180));
	}
	
	public void addLabel(double latitude, double longitude, String label) {
//		System.out.println(latitude+" "+label);
		if (label == null) return;
		labels.put(new FloatPoint((float)longitude, (float)latitude), label);
		redraw();
	}
}
