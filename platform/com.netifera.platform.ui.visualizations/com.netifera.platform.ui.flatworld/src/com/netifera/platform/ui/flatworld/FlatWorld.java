package com.netifera.platform.ui.flatworld;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistable;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import com.netifera.platform.ui.flatworld.support.FloatRectangle;
import com.netifera.platform.ui.internal.flatworld.Activator;

public class FlatWorld extends Canvas implements IPersistable {
	
	private Image texture;
	private Image textureSmall;

	private List<IFlatWorldLayer> layers;
	
	private Cursor panningCursor;
	private Cursor zoomingCursor;
	
	class Frame {
		float scale = 1.0f, offsetX = 0, offsetY = 0;
		
		void adjust() {
			Rectangle textureBounds = texture.getBounds();
			float srcX = textureBounds.x + frame.offsetX;
			float srcY = textureBounds.y + frame.offsetY;
			float srcWidth = textureBounds.width / frame.scale;
			float srcHeight = textureBounds.height / frame.scale;
			if (srcX + srcWidth > textureBounds.x + textureBounds.width)
				offsetX -= srcX + srcWidth - (textureBounds.x + textureBounds.width);
			if (srcY + srcHeight > textureBounds.y + textureBounds.height)
				offsetY -= srcY + srcHeight - (textureBounds.y + textureBounds.height);
			if (offsetX < 0) offsetX = 0;
			if (offsetY < 0) offsetY = 0;
		}
	};
	
	private Frame frame = new Frame();
	
	public FlatWorld(Composite parent, int style) {
		super(parent, style);

		initializeCursors();
		initializeTexture();

		reset();
		
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
			float originalScale, originalOffsetX, originalOffsetY;
			
			boolean panning = false;
			boolean zooming = false;
			
			public void handleEvent(Event event) {
				switch (event.type) {
				case SWT.MouseDoubleClick:
					if (event.button == 2 || event.button == 3) {
						frame.offsetX = 0;
						frame.offsetY = 0;
						frame.scale = 1.0f;
						redraw();
						zooming = false;
						panning = false;
						FlatWorld.this.setCursor(null);
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
						FlatWorld.this.setCursor(panningCursor);
					}
					if (!zooming && event.button == 3) {
						clickX = event.x;
						clickY = event.y;
						originalOffsetX = frame.offsetX;
						originalOffsetY = frame.offsetY;
						originalScale = frame.scale;
						zooming = true;
						FlatWorld.this.setCursor(zoomingCursor);
					}
					break;
				case SWT.MouseMove:
					if (panning) {
						Rectangle rect = getClientArea();
						Rectangle textureBounds = texture.getBounds();
						frame.offsetX = originalOffsetX + ((clickX - event.x)/frame.scale)*textureBounds.width/rect.width;
						frame.offsetY = originalOffsetY + ((clickY - event.y)/frame.scale)*textureBounds.height/rect.height;
						frame.adjust();
						redraw();
					}
					if (zooming) {
						frame.scale = (float) Math.max(1.0, originalScale * Math.pow(2.0, (clickY - event.y) / 10.0));
						float lambda = 1.0f/originalScale - 1.0f/frame.scale;
						Rectangle rect = getClientArea();
						Rectangle textureBounds = texture.getBounds();
						frame.offsetX = originalOffsetX + (clickX*textureBounds.width*lambda)/rect.width;
						frame.offsetY = originalOffsetY + (clickY*textureBounds.height*lambda)/rect.height;
						frame.adjust();
						redraw();
					}
					break;
				case SWT.MouseUp:
					clickX = null;
					clickY = null;
					panning = false;
					zooming = false;
					FlatWorld.this.setCursor(null);
					break;
				case SWT.MouseWheel:
					originalScale = frame.scale;
					frame.scale = (float) Math.max(1.0, originalScale * Math.pow(2.0, event.count / 10.0));
					float lambda = 1.0f/originalScale - 1.0f/frame.scale;
					Rectangle rect = getClientArea();
					Rectangle textureBounds = texture.getBounds();
					frame.offsetX = frame.offsetX + (event.x*textureBounds.width*lambda)/rect.width;
					frame.offsetY = frame.offsetY + (event.y*textureBounds.height*lambda)/rect.height;
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

	private void initializeTexture() {
		ImageDescriptor textureDescriptor;
//		textureDescriptor = AbstractUIPlugin.imageDescriptorFromPlugin(Activator.PLUGIN_ID, "textures/earth_lights.png");
//		textureDescriptor = AbstractUIPlugin.imageDescriptorFromPlugin(Activator.PLUGIN_ID, "textures/earth_lights_lrg.jpg");
		textureDescriptor = AbstractUIPlugin.imageDescriptorFromPlugin(Activator.PLUGIN_ID, "textures/world.topo.200408.3x5400x2700.jpg");
//		textureDescriptor = AbstractUIPlugin.imageDescriptorFromPlugin(Activator.PLUGIN_ID, "textures/srtm_ramp2.world.5400x2700.jpg");
//		textureDescriptor = AbstractUIPlugin.imageDescriptorFromPlugin(Activator.PLUGIN_ID, "textures/world.shoreline.4000x2000.png");
		texture = textureDescriptor.createImage();
		
		Rectangle textureBounds = texture.getBounds();
		int width = textureBounds.width / 3;
		int height = textureBounds.height / 3;
		textureSmall = new Image(getDisplay(), width, height);
		GC gc = new GC(textureSmall);
		gc.drawImage(texture, textureBounds.x, textureBounds.y, textureBounds.width, textureBounds.height, 0, 0, width, height);
	}

	private void initializeCursors() {
		panningCursor = new Cursor(getShell().getDisplay(), SWT.CURSOR_SIZEALL);
		zoomingCursor = new Cursor(getShell().getDisplay(), SWT.CURSOR_SIZENS);
	}

	public void reset() {
		layers = new ArrayList<IFlatWorldLayer>();
	}
	
	public void addLayer(IFlatWorldLayer layer) {
		layers.add(layer);
	}
	
	@Override
	public void dispose() {
		texture.dispose();
		textureSmall.dispose();
		panningCursor.dispose();
		zoomingCursor.dispose();
		super.dispose();
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
		if (frame.scale > 3)
			paint(event, texture, 1);
		else
			paint(event, textureSmall, 3);
	}
	
	private void paint(PaintEvent event, Image texture, int textureScale) {
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
		float pixelWidth = rect.width * frame.scale / textureBounds.width;
		float pixelHeight = rect.height * frame.scale / textureBounds.height;

		float ox = frame.offsetX / textureScale;
		float oy = frame.offsetY / textureScale;
		
		int srcX = (int) (textureBounds.x + ox);
		int srcY = (int) (textureBounds.y + oy);
		
		float dtx = ox - (int) ox;
		float dty = oy - (int) oy;
		
		int dx = (int) (pixelWidth * dtx);
		int dy = (int) (pixelHeight * dty);
		
		int srcWidth = Math.min((int)(textureBounds.width / frame.scale + 2), textureBounds.width-srcX);
		int srcHeight = Math.min((int)(textureBounds.height / frame.scale + 2), textureBounds.height-srcY);

		int w = (int) (pixelWidth * srcWidth);
		int h = (int) (pixelHeight * srcHeight);
		
		gc.drawImage(texture, srcX, srcY, srcWidth, srcHeight, rect.x-dx, rect.y-dy, w, h);

		final FloatRectangle region = getVisibleGeographicalRegion();
		
		for (IFlatWorldLayer layer: layers)
			layer.paint(region, rect, gc);
		
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

	private FloatRectangle getVisibleGeographicalRegion() {
		Rectangle textureBounds = texture.getBounds();
		float srcX = textureBounds.x + frame.offsetX;
		float srcY = textureBounds.y + frame.offsetY;
		float srcWidth = textureBounds.width / frame.scale;
		float srcHeight = textureBounds.height / frame.scale;
		return getGeographicalRegionFromTextureRegion(srcX, srcY, srcWidth, srcHeight);
	}
	
	private FloatRectangle getGeographicalRegionFromTextureRegion(float x, float y, float width, float height) {
		Rectangle textureBounds = texture.getBounds();
		return new FloatRectangle((x-textureBounds.x)*360/textureBounds.width-180,(y-textureBounds.y)*-180/textureBounds.height+90 - 180*height/textureBounds.height,360*width/textureBounds.width,180*height/textureBounds.height);
	}
/*
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
*/
}
