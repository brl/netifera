package com.netifera.platform.ui.navigator;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.graphics.Image;

import com.netifera.platform.api.model.ISpace;
import com.netifera.platform.api.probe.IProbe;
import com.netifera.platform.ui.images.ImageCache;

public class NavigatorLabelProvider extends ColumnLabelProvider {

	private final static String SPACE_OPEN = "icons/space.png";
	private final static String SPACE_CLOSED = "icons/space_gray.png";
	private final static String ISOLATED_SPACE_OPEN = "icons/space_isolated.png";
	private final static String ISOLATED_SPACE_CLOSED = "icons/space_isolated_gray.png";
	
	private final static String PROBE_DISCONNECTED = "icons/probe_disconnected.png";
	private final static String PROBE_CONNECTING = "icons/probe_connecting.png";
	private final static String PROBE_CONNECTED = "icons/probe_connected.png";
	private final static String PROBE_FAILED = "icons/probe_failed.png";
	
	public final static String PROBE_PLUGIN_ID = "com.netifera.platform.ui.probe";
	public final static String SPACES_PLUGIN_ID = "com.netifera.platform.ui.spaces";

	private final ImageCache probeImages = new ImageCache(PROBE_PLUGIN_ID);
	private final ImageCache spaceImages = new ImageCache(SPACES_PLUGIN_ID);

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
			return getSpaceImage((ISpace)element);
		}
		if (element instanceof IProbe) {
			return getProbeStatusImage((IProbe)element);
		}
		return null;
	}

	private Image getSpaceImage(ISpace space) {
		if(space.isOpened()) {
			return spaceImages.get(space.isIsolated() ? ISOLATED_SPACE_OPEN : SPACE_OPEN);
		} else {
			return spaceImages.get(space.isIsolated() ? ISOLATED_SPACE_CLOSED : SPACE_CLOSED);
		}
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
	
	public void dispose() {
		super.dispose();
		probeImages.dispose();
		spaceImages.dispose();
	}
}
