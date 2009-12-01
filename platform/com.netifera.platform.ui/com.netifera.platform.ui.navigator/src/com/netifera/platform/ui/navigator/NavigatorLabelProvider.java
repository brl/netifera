package com.netifera.platform.ui.navigator;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.swt.graphics.Image;

import com.netifera.platform.api.model.ISpace;
import com.netifera.platform.api.probe.IProbe;
import com.netifera.platform.ui.images.ImageCache;

public class NavigatorLabelProvider extends ColumnLabelProvider {

	private final static String SPACE_ICON = "icons/space.png";
	private final static String SPACE_CLOSED_ICON = "icons/space_gray.png";
	private final static String SPACE_ISOLATED_ICON = "icons/space_isolated.png";
	private final static String SPACE_ISOLATED_CLOSED_ICON = "icons/space_isolated_gray.png";
	private final static String SPACE_ACTIVE_OVERLAY = "icons/space_active_overlay.png";
	
	private final static String PROBE_DISCONNECTED_ICON = "icons/probe_disconnected.png";
	private final static String PROBE_CONNECTING_ICON = "icons/probe_connecting.png";
	private final static String PROBE_CONNECTED_ICON = "icons/probe_connected.png";
	private final static String PROBE_FAILED_ICON = "icons/probe_failed.png";
	
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
			String baseIcon = space.isIsolated() ? SPACE_ISOLATED_ICON : SPACE_ICON;
			if (space.isActive()) {
				String overlayKeys[] = new String[5];
				overlayKeys[IDecoration.TOP_RIGHT] = SPACE_ACTIVE_OVERLAY;
				return spaceImages.getDecorated(baseIcon, overlayKeys);
			}
			return spaceImages.get(baseIcon);
		} else {
			return spaceImages.get(space.isIsolated() ? SPACE_ISOLATED_CLOSED_ICON : SPACE_CLOSED_ICON);
		}
	}
	
	private Image getProbeStatusImage(IProbe probe) {
		switch(probe.getConnectState()) {
		case CONNECTED:
			return probeImages.get(PROBE_CONNECTED_ICON);
		case CONNECTING:
			return probeImages.get(PROBE_CONNECTING_ICON);
		case DISCONNECTED:
			return probeImages.get(PROBE_DISCONNECTED_ICON);
		case CONNECT_FAILED:
			return probeImages.get(PROBE_FAILED_ICON);
		
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
