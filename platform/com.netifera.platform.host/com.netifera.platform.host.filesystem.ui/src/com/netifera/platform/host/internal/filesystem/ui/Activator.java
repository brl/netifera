package com.netifera.platform.host.internal.filesystem.ui;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import com.netifera.platform.services.IServiceFactory;
import com.netifera.platform.ui.images.ImageCache;
import com.netifera.platform.ui.util.BalloonManager;

public class Activator implements BundleActivator {
	public final static String PLUGIN_ID = "com.netifera.platform.host.filesystem.ui";

	private static Activator instance;
	private BalloonManager balloonManager;
	private ImageCache imageCache;
	
	private ServiceTracker serviceFactoryTracker;


	public static Activator getInstance() {
		return instance;
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		instance = this;
		imageCache = new ImageCache(PLUGIN_ID);
		
		serviceFactoryTracker = new ServiceTracker(context, IServiceFactory.class.getName(), null);
		serviceFactoryTracker.open();
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
//		balloonManager.dispose();
		imageCache.dispose();
		imageCache = null;
	}

	public synchronized BalloonManager getBalloonManager() {
		if (balloonManager == null)
			balloonManager = new BalloonManager();
		return balloonManager;
	}
	
	public ImageCache getImageCache() {
		return imageCache;
	}
	
	public IServiceFactory getServiceFactory() {
		return (IServiceFactory) serviceFactoryTracker.getService();
	}
}
