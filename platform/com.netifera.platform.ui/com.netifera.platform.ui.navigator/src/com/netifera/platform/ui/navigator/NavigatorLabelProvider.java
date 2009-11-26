package com.netifera.platform.ui.navigator;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.graphics.Image;

import com.netifera.platform.api.model.ISpace;
import com.netifera.platform.api.probe.IProbe;
import com.netifera.platform.ui.images.ImageCache;
import com.netifera.platform.ui.internal.navigator.Activator;

public class NavigatorLabelProvider extends ColumnLabelProvider {

	private final static String SPACE_OPENED = "icons/space.png";
	private final static String SPACE_CLOSED = "icons/space_gray.png";
	
	private final static String PROBE_DISCONNECTED = "icons/probe_disconnected.png";
	private final static String PROBE_CONNECTING = "icons/probe_connecting.png";
	private final static String PROBE_CONNECTED = "icons/probe_connected.png";
	private final static String PROBE_FAILED = "icons/probe_failed.png";
	
	public final static String PROBE_PLUGIN_ID = "com.netifera.platform.ui.probe";

	private final ImageCache probeImages = new ImageCache(PROBE_PLUGIN_ID);

	@Override
	public String getText(Object element) {
		if (element instanceof ISpace) {
			ISpace space = (ISpace) element;
			return space.getName() + " (" + space.entityCount() + ")";
		}
		if (element instanceof IProbe) {
			return ((IProbe)element).getName();
		}
		return null;
	}
	
	@Override
	public Image getImage(Object element) {
		if (element instanceof ISpace) {
			ISpace space = (ISpace) element;
			if(space.isOpened()) {
				return Activator.getInstance().getImageCache().get(SPACE_OPENED);
			} else {
				return Activator.getInstance().getImageCache().get(SPACE_CLOSED);
			}
		}
		if (element instanceof IProbe) {
			return getProbeStatusImage((IProbe)element);
		}
		return null;
	}
	
	private Image getProbeStatusImage(IProbe probe) {
		switch(probe.getConnectState()) {
		case CONNECTED:
			return probeImages.get(PROBE_CONNECTED);
		case CONNECTING:
			return probeImages.get(PROBE_CONNECTING);
		case DISCONNECTED:
			return probeImages.get(PROBE_DISCONNECTED);
		case CONNECT_FAILED:
			return probeImages.get(PROBE_FAILED);
		
		default:
			return null;
		}
	}
}
