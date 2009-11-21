package com.netifera.platform.ui.internal.flatworld;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import com.netifera.platform.api.model.IModelService;
import com.netifera.platform.net.geoip.IGeoIPService;
import com.netifera.platform.ui.api.model.IEntityLabelProviderService;
import com.netifera.platform.ui.images.ImageCache;


public class Activator extends AbstractUIPlugin {

	public static final String PLUGIN_ID = "com.netifera.platform.ui.flatworld";

	private static Activator instance;

	public static Activator getInstance() {
		return instance;
	}

	private ImageCache imageCache;

	private ServiceTracker modelTracker;
	private ServiceTracker modelLabelsTracker;
	private ServiceTracker geoipServiceTracker;
	

	public Activator() {
	}

	public void start(BundleContext context) throws Exception {
		super.start(context);
		instance = this;
		
		imageCache = new ImageCache(PLUGIN_ID);
		
		modelTracker = new ServiceTracker(context, IModelService.class.getName(), null);
		modelTracker.open();
		
		modelLabelsTracker = new ServiceTracker(context, IEntityLabelProviderService.class.getName(), null);
		modelLabelsTracker.open();
		
		geoipServiceTracker = new ServiceTracker(context, IGeoIPService.class.getName(), null);
		geoipServiceTracker.open();
	}
	
	public void stop(BundleContext context) throws Exception {
		imageCache.dispose();
		imageCache = null;
		instance = null;
		super.stop(context);
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
	
	public ImageCache getImageCache() {
		return imageCache;
	}
}
