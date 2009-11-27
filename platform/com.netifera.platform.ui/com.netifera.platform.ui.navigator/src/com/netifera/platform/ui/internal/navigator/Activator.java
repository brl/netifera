package com.netifera.platform.ui.internal.navigator;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import com.netifera.platform.api.log.ILogManager;
import com.netifera.platform.api.model.IModelService;
import com.netifera.platform.api.probe.IProbeManagerService;
import com.netifera.platform.ui.api.hover.IHoverService;
import com.netifera.platform.ui.api.inputbar.IInputBarActionProviderService;
import com.netifera.platform.ui.api.model.IEntityLabelProviderService;
import com.netifera.platform.ui.images.ImageCache;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "com.netifera.platform.ui.navigator";

	// The shared instance
	private static Activator instance;

	public static Activator getInstance() {
		return instance;
	}

	private ServiceTracker modelTracker;
	private ServiceTracker probeManagerTracker;
	private ServiceTracker modelLabelsTracker;
	private ServiceTracker actionProviderServiceTracker;
	private ServiceTracker inputBarActionProviderTracker;
	private ServiceTracker logManagerTracker;

	private ImageCache imageCache;

	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		instance = this;
		
		imageCache = new ImageCache(PLUGIN_ID);
		
		modelTracker = new ServiceTracker(context, IModelService.class.getName(), null);
		modelTracker.open();
		
		probeManagerTracker = new ServiceTracker(context, IProbeManagerService.class.getName(), null);
		probeManagerTracker.open();
		
		modelLabelsTracker = new ServiceTracker(context, IEntityLabelProviderService.class.getName(), null);
		modelLabelsTracker.open();
		
		actionProviderServiceTracker = new ServiceTracker(context, IHoverService.class.getName(), null);
		actionProviderServiceTracker.open();
		
		inputBarActionProviderTracker = new ServiceTracker(context, IInputBarActionProviderService.class.getName(), null);
		inputBarActionProviderTracker.open();
		
		logManagerTracker = new ServiceTracker(context, ILogManager.class.getName(), null);
		logManagerTracker.open();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		imageCache.dispose();
		imageCache = null;
		instance = null;
		super.stop(context);
	}

	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}
	
	
	public IModelService getModel() {
		return (IModelService) modelTracker.getService();
	}
	
	public IProbeManagerService getProbeManager() {
		return (IProbeManagerService) probeManagerTracker.getService();
	}
	
	public IEntityLabelProviderService getLabelProvider() {
		return (IEntityLabelProviderService) modelLabelsTracker.getService();
	}
	
	public IHoverService getActionProvider() {
		return (IHoverService) actionProviderServiceTracker.getService();
	}
	
	public IInputBarActionProviderService getInputBarActionProvider() {
		return (IInputBarActionProviderService) inputBarActionProviderTracker.getService();
	}
	
	public ILogManager getLogManager() {
		return (ILogManager) logManagerTracker.getService();
	}
	
	public ImageCache getImageCache() {
		return imageCache;
	}
}
