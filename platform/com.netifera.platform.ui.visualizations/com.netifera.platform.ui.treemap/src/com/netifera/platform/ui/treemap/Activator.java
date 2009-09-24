package com.netifera.platform.ui.treemap;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import com.netifera.platform.api.model.IModelService;
import com.netifera.platform.ui.images.ImageCache;

public class Activator implements BundleActivator {

	public static final String PLUGIN_ID = "com.netifera.platform.ui.world";

	// The shared instance
	private static Activator plugin;

	public static Activator getInstance() {
		return plugin;
	}

	private ImageCache imageCache;
	
	private ServiceTracker modelTracker;

	
	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		plugin = this;

		modelTracker = new ServiceTracker(context, IModelService.class.getName(), null);
		modelTracker.open();

		imageCache = new ImageCache(PLUGIN_ID);
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		imageCache.dispose();
		imageCache = null;
		plugin = null;
	}

	public IModelService getModel() {
		return (IModelService) modelTracker.getService();
	}

	public ImageCache getImageCache() {
		return imageCache;
	}
}
