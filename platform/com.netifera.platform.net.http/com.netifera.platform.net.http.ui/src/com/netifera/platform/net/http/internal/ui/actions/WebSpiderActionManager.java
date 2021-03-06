package com.netifera.platform.net.http.internal.ui.actions;

import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.ControlContribution;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.PlatformUI;

import com.netifera.platform.api.events.IEvent;
import com.netifera.platform.api.events.IEventHandler;
import com.netifera.platform.api.probe.IProbe;
import com.netifera.platform.net.http.internal.ui.Activator;
import com.netifera.platform.net.http.internal.ui.inputbar.WebInputBar;
import com.netifera.platform.net.http.spider.daemon.IWebSpiderDaemon;

public class WebSpiderActionManager {
	private final RGB WARNING_COLOR = new RGB(0xF5, 0xA9, 0xA9);

	private final StopAction stopAction;
	private final ConfigureAction configureAction;
//	private final FetchAction fetchAction;
	private final GoAction goAction;
	
	// The toolbar contributions, so we can add and remove them.
	private final IContributionItem stopItem;
	private final WebInputBar inputBar;
	private final ActionContributionItem goItem;
	private final ActionContributionItem configItem;

	private IToolBarManager toolbarManager;
	private IEventHandler probeEventHandler;
	private IWebSpiderDaemon currentWebSpiderDaemon;
	private IEventHandler changeHandler;
	
	public WebSpiderActionManager(IToolBarManager toolbarManager) {
		this.toolbarManager = toolbarManager;
		
		configureAction = new ConfigureAction(this);
		configItem = new ActionContributionItem(configureAction);

		inputBar = new WebInputBar(this);
		
		goAction = new GoAction(inputBar);
		goItem = new ActionContributionItem(goAction);

		stopAction = new StopAction(this);
		stopItem = new ActionContributionItem(stopAction);
		
		toolbarManager.add(configItem);
		
		toolbarManager.add(stopItem);
		toolbarManager.add(inputBar);
		toolbarManager.add(goItem);
		
		changeHandler = new IEventHandler() {
			public void handleEvent(IEvent event) {
				setState();				
			}
		};
		
		currentWebSpiderDaemon = Activator.getDefault().createWebSpiderDaemon(changeHandler);
		
		addProbeChangeListener();
	}
	
	private void addProbeChangeListener() {
		
		probeEventHandler = new IEventHandler() {
			public void handleEvent(IEvent event) {
				PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
					public void run() {
						currentWebSpiderDaemon = Activator.getDefault().createWebSpiderDaemon(changeHandler);
						setState();						
					}
				});
			}
		};
		Activator.getDefault().getProbeManager().addProbeChangeListener(probeEventHandler);
	}
	
	public void dispose() {
		Activator.getDefault().getProbeManager().removeProbeChangeListener(probeEventHandler);
	}
	
	public IToolBarManager getToolBar() {
		return toolbarManager;
	}

	public void setFailed(final String message) {
		
		IContributionItem spiderLabelItem = new ControlContribution("spider_text") {
			@Override
			protected Control createControl(Composite parent) {
				final CLabel label = new CLabel(parent, SWT.CENTER);
				label.setBackground(new Color(parent.getDisplay(), WARNING_COLOR));
				label.setText("   " + message + "   ");
				label.pack(true);
				return label;
			}
			
		};
		
		toolbarManager.insertAfter(GoAction.ID, spiderLabelItem);
		toolbarManager.update(true);		
	}
	
	public void asynchSetState() {
		if(PlatformUI.getWorkbench().getDisplay().isDisposed())
			return;
		
		PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
			public void run() {
				setState();	
			}
		});
	}
	
	public void setState() {
		removeLabelIfExists();
		enableAll();
		
		final IProbe probe = Activator.getDefault().getCurrentProbe();
		if(probe == null) {
			failAll("No probe found!");
			return;
		}
		
		if(!probe.isConnected()) {
			failAll("Probe for this space is currently disconnected");
			return;
		}
		
		final IWebSpiderDaemon daemon = Activator.getDefault().getWebSpiderDaemon();

		if(daemon == null) {
			failAll("No web spider service found");
			return;
		}
		
/*		final WebSpiderConfiguration config = daemon.getConfiguration();
		
		if(config == null) {
			failAll("No web spider found on remote probe");
			return;
		}
*/		
		if(!daemon.isRunning()) {
			stopAction.setEnabled(false);
		}
			
	}
	
	private void removeLabelIfExists() {
		final IContributionItem item = toolbarManager.remove("sniffer_text");
		if(item != null) {
			item.dispose();
			toolbarManager.update(true);
		}
	}
	private void failAll(String message) {
		setFailed(message);
		disableAll();
	}
	
	public void disableAll() {
		stopAction.setEnabled(false);
//		inputBar.setEnabled(false);
		configureAction.setEnabled(false);
//		fetchAction.setEnabled(false);
	}
	
	private void enableAll() {
		stopAction.setEnabled(true);
//		inputBar.setEnabled(true);
		configureAction.setEnabled(true);
//		fetchAction.setEnabled(true);
	}
	
	public Point getConfigDialogLocation() {
		 Widget widget = configItem.getWidget();
		 if(!(widget instanceof ToolItem))
			 return null;
		 ToolItem item = (ToolItem) widget;
			 
		 int x = item.getBounds().x;
		 int y = item.getBounds().y + item.getBounds().height;
		 Point p = item.getDisplay().map(item.getParent(), null, x, y);
		 return p;
	}
}
