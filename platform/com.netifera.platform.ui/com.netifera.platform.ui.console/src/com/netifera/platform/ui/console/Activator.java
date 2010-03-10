package com.netifera.platform.ui.console;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import com.netifera.platform.api.log.ILogManager;
import com.netifera.platform.ui.images.ImageCache;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	public static final String PLUGIN_ID = "com.netifera.platform.ui.console";

	private static Activator instance;

	public static Activator getDefault() {
		return instance;
	}

	private ImageCache imageCache;

	private ServiceTracker logManagerTracker;

	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		instance = this;

		imageCache = new ImageCache(PLUGIN_ID);

		logManagerTracker = new ServiceTracker(context, ILogManager.class.getName(), null);
		logManagerTracker.open();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		instance = null;
		imageCache.dispose();
		super.stop(context);
	}

	public ImageCache getImageCache() {
		return imageCache;
	}
	
	public ILogManager getLogManager() {
		return (ILogManager) logManagerTracker.getService();
	}
}
