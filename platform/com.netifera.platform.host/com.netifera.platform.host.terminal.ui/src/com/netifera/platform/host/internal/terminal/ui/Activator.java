package com.netifera.platform.host.internal.terminal.ui;

import org.eclipse.core.runtime.Platform;
import org.eclipse.tm.internal.terminal.provisional.api.Logger;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import com.netifera.platform.services.IServiceFactory;
import com.netifera.platform.ui.images.ImageCache;

public class Activator extends AbstractUIPlugin {
	public final static String PLUGIN_ID = "com.netifera.platform.host.terminal.ui";
	
	private static Activator instance;
//	public static final String HELPPREFIX = "com.netifera.platform.terminal.view."; //$NON-NLS-1$

	private final ImageCache imageCache = new ImageCache(PLUGIN_ID);

	private ServiceTracker serviceFactoryTracker;
	
	public static Activator getInstance() {
		return instance;
	}
	
	@Override
	public void start(final BundleContext context) throws Exception {
		instance = this;
		super.start(context);
		
		serviceFactoryTracker = new ServiceTracker(context, IServiceFactory.class.getName(), null);
		serviceFactoryTracker.open();
	}

	@Override
	public void stop(final BundleContext context) throws Exception {
		super.stop(context);
		instance = null;
	}

	public static boolean isLogInfoEnabled() {
		return isOptionEnabled(Logger.TRACE_DEBUG_LOG_INFO);
	}
	public static boolean isLogErrorEnabled() {
		return isOptionEnabled(Logger.TRACE_DEBUG_LOG_ERROR);
	}
	public static boolean isLogEnabled() {
		return isOptionEnabled(Logger.TRACE_DEBUG_LOG);
	}

	public static boolean isOptionEnabled(final String strOption) {
		String strEnabled;
		Boolean boolEnabled;
		boolean bEnabled;

		strEnabled = Platform.getDebugOption(strOption);
		if (strEnabled == null)
			return false;

		boolEnabled = Boolean.valueOf(strEnabled);
		bEnabled = boolEnabled.booleanValue();

		return bEnabled;
	}
	
	public ImageCache getImageCache() {
		return imageCache;
	}
	
	public IServiceFactory getServiceFactory() {
		return (IServiceFactory) serviceFactoryTracker.getService();
	}
}
