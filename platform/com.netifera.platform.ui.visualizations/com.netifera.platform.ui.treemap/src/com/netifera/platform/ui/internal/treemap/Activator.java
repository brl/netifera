package com.netifera.platform.ui.internal.treemap;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import com.netifera.platform.api.model.IModelService;
import com.netifera.platform.net.ui.geoip.IGeoIPService;
import com.netifera.platform.net.ui.routes.IIP2ASService;
import com.netifera.platform.ui.api.model.IEntityLabelProviderService;
import com.netifera.platform.ui.images.ImageCache;

public class Activator implements BundleActivator {

	public static final String PLUGIN_ID = "com.netifera.platform.ui.treemap";

	private static Activator instance;

	public static Activator getInstance() {
		return instance;
	}
	
	private ImageCache imageCache;

	private ServiceTracker modelTracker;
	private ServiceTracker modelLabelsTracker;
	private ServiceTracker geoipServiceTracker;
	private ServiceTracker ip2asServiceTracker;
	

	public Activator() {
	}

	public void start(BundleContext context) throws Exception {
		instance = this;
		
		imageCache = new ImageCache(PLUGIN_ID);
		
		modelTracker = new ServiceTracker(context, IModelService.class.getName(), null);
		modelTracker.open();
		
		modelLabelsTracker = new ServiceTracker(context, IEntityLabelProviderService.class.getName(), null);
		modelLabelsTracker.open();
		
		geoipServiceTracker = new ServiceTracker(context, IGeoIPService.class.getName(), null);
		geoipServiceTracker.open();
		
		ip2asServiceTracker = new ServiceTracker(context, IIP2ASService.class.getName(), null);
		ip2asServiceTracker.open();
	}
	
	public void stop(BundleContext context) throws Exception {
		imageCache.dispose();
		imageCache = null;
		instance = null;
	}

	public IModelService getModel() {
		return (IModelService) modelTracker.getService();
	}
	
	public IEntityLabelProviderService getLabelProvider() {
		return (IEntityLabelProviderService) modelLabelsTracker.getService();
	}
	
	public IGeoIPService getGeoIPService() {
		return (IGeoIPService) geoipServiceTracker.getService();
	}

	public IIP2ASService getIP2ASService() {
		return (IIP2ASService) ip2asServiceTracker.getService();
	}

	public ImageCache getImageCache() {
		return imageCache;
	}
}
