package com.netifera.platform.net.daemon.sniffing.ui;


import java.util.Collection;

import org.eclipse.jface.action.ICoolBarManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.ToolBarContributionItem;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import com.netifera.platform.api.events.IEventHandler;
import com.netifera.platform.api.log.ILogManager;
import com.netifera.platform.api.model.ISpace;
import com.netifera.platform.api.probe.IProbe;
import com.netifera.platform.api.probe.IProbeManagerService;
import com.netifera.platform.api.system.ISystemService;
import com.netifera.platform.net.daemon.sniffing.ISniffingDaemon;
import com.netifera.platform.net.daemon.sniffing.ISniffingDaemonFactory;
import com.netifera.platform.net.pcap.ICaptureInterface;
import com.netifera.platform.system.privd.IPrivilegeDaemon;
import com.netifera.platform.system.privd.IPrivilegeDaemonLaunchStatus.StatusType;
import com.netifera.platform.ui.application.ApplicationPlugin;
import com.netifera.platform.ui.spaces.SpaceEditorInput;
import com.netifera.platform.ui.workbench.IWorkbenchChangeListener;
import com.netifera.platform.ui.workbench.WorkbenchChangeManager;


public class Activator extends AbstractUIPlugin {

	public static final String PLUGIN_ID = "com.netifera.platform.net.daemon.sniffing.ui";
	
	public static final String PERSPECTIVE_ID = "com.netifera.platform.ui.perspectives.sniffing";
	public static final String MODEL_VIEW_ID = "com.netifera.platform.ui.views.Model";

	private static Activator plugin;
	
	
	private ServiceTracker sniffingDaemonFactoryTracker;
	private ServiceTracker probeManagerTracker;
	private ServiceTracker logManagerTracker;
	private ServiceTracker systemServiceTracker;
	private ServiceTracker privdTracker;
	
	private SniffingActionManager sniffingActionManager;
	private ToolBarContributionItem toolbarItem;
	
	private WorkbenchChangeManager workbenchChangeManager;
	
	public Activator() {
		
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		
		sniffingDaemonFactoryTracker = new ServiceTracker(context, ISniffingDaemonFactory.class.getName() , null);
		sniffingDaemonFactoryTracker.open();
		
		probeManagerTracker = new ServiceTracker(context, IProbeManagerService.class.getName(), null);
		probeManagerTracker.open();
		
		logManagerTracker = new ServiceTracker(context, ILogManager.class.getName(), null);
		logManagerTracker.open();

		systemServiceTracker = new ServiceTracker(context,ISystemService.class.getName(),null);
		systemServiceTracker.open();

		privdTracker = new ServiceTracker(context, IPrivilegeDaemon.class.getName(), null);
		privdTracker.open();
	}

	public ISniffingDaemonFactory getSniffingDaemonFactory() {
		return (ISniffingDaemonFactory) sniffingDaemonFactoryTracker.getService();
	}
	
	public IProbeManagerService getProbeManager() {
		return (IProbeManagerService) probeManagerTracker.getService();
	}
	
	public ILogManager getLogManager() {
		return (ILogManager) logManagerTracker.getService();
	}
	
	public ISystemService getSystemService() {
		return (ISystemService) systemServiceTracker.getService();
	}
	
	public IPrivilegeDaemon getPrivilegeDaemon() {
		return (IPrivilegeDaemon) privdTracker.getService();
	}

	public ISniffingDaemon getSniffingDaemon() {
		IProbe probe = getCurrentProbe();
		if(probe == null)
			return null;

		// XXX fix with change handler
		return getSniffingDaemonFactory().createForProbe(probe, null);
	}
	
	public ISniffingDaemon createSniffingDaemon(IEventHandler changeHandler) {
		IProbe probe = getCurrentProbe();
		if(probe == null)
			return null;
		return getSniffingDaemonFactory().createForProbe(probe, changeHandler);
	}
	
	public void initialize() {
		workbenchChangeManager = new WorkbenchChangeManager(getWindow(), PerspectiveFactory.ID, createChangeListener());
		workbenchChangeManager.initialize();
	}
	
	private IWorkbenchChangeListener createChangeListener() {
		return new IWorkbenchChangeListener() {

			public void activePageOpened(IWorkbenchPage page) {	
			}

			public void partChange() {
				if(sniffingActionManager != null) {
					sniffingActionManager.setState();
				}	
			}

			public void perspectiveClosed() {
				if(toolbarItem != null) {
					ApplicationPlugin.getDefault().getCoolBar().remove(toolbarItem);
					toolbarItem.dispose();
					toolbarItem = null;
					if(sniffingActionManager != null) {
						sniffingActionManager.dispose();
						sniffingActionManager = null;
					}
				}
				
			}

			public void perspectiveOpened() {
				displayToolbar();
			}
			
		};
	}

	private void displayToolbar() {
		if(toolbarItem != null)
			return;
		final ICoolBarManager coolbar = ApplicationPlugin.getDefault().getCoolBar();
		final IToolBarManager toolbar = new ToolBarManager(coolbar.getStyle());
		toolbarItem = new ToolBarContributionItem(toolbar);
		coolbar.add(toolbarItem);
		sniffingActionManager = new SniffingActionManager(toolbar);
		PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
			public void run() {
				checkPrivdAuthentication();
				sniffingActionManager.setState();
				coolbar.update(true);
				
			}		
		});	
	}
	
	private void checkPrivdAuthentication() {
		final IPrivilegeDaemon privd = getPrivilegeDaemon();
		if(privd == null || privd.getDaemonLaunchStatus().getStatusType() != StatusType.WAITING_AUTHENTICATION)
			return;
		String promptMessage = "Enter password to authenticate to privilege daemon";
		while(true) {
			final InputDialog diag = new InputDialog(null, "Privilege Daemon", promptMessage, "", null);

			if(diag.open() != Dialog.OK)
				return;
			if(privd.authenticate(diag.getValue())) {
				refreshSniffingInterfaces();
				return;
			} else {
				promptMessage = "Authentication failed.  Try again.";
			}
		}
	}
	
	private void refreshSniffingInterfaces() {
		final ISniffingDaemon sniffingDaemon = getSniffingDaemon();
		if(sniffingDaemon == null) 
			return;
		sniffingDaemon.refreshInterfaces();
		final Collection<ICaptureInterface> interfaces = sniffingDaemon.getInterfaces();
		for(ICaptureInterface i : interfaces) {
			if(i.captureAvailable())
				sniffingDaemon.setEnabled(i, true);
		}
	}
	public ISpace getCurrentSpace() {
		IWorkbenchPage page = getWindow().getActivePage();
		if(page == null) {
			return null;
		}
		IEditorPart editor = page.getActiveEditor();
		if(editor == null || !(editor.getEditorInput() instanceof SpaceEditorInput)) {
			return null;
		}
		
		return ((SpaceEditorInput)editor.getEditorInput()).getSpace();
	}

	public IProbe getCurrentProbe() {
		final ISpace space = getCurrentSpace();
		if(space == null)
			return null;
		return getProbeManager().getProbeById(getCurrentSpace().getProbeId());
	}

	private IWorkbenchWindow getWindow() {
		IWorkbenchWindow[] windows = getWorkbench().getWorkbenchWindows();
		if(windows.length == 0) {
			return null;
		}
		return windows[0];
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	public static Activator getDefault() {
		return plugin;
	}

}
