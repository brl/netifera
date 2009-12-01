package com.netifera.platform.ui.application;

import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPreferenceConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;
import org.eclipse.ui.handlers.IHandlerService;

public class ApplicationWorkbenchWindowAdvisor extends WorkbenchWindowAdvisor {

    public ApplicationWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer) {
        super(configurer);
    }

    public ActionBarAdvisor createActionBarAdvisor(IActionBarConfigurer configurer) {
        return new ApplicationActionBarAdvisor(configurer);
    }
    
    public void preWindowOpen() {
        IWorkbenchWindowConfigurer configurer = getWindowConfigurer();
        configurer.setInitialSize(new Point(1200, 900));
        configurer.setShowCoolBar(true);
        configurer.setShowStatusLine(true);
        configurer.setShowPerspectiveBar(true);
        configurer.setShowFastViewBars(true);
//        configurer.setShowProgressIndicator(true);
        
//		PlatformUI.getPreferenceStore().setValue( 
//				IWorkbenchPreferenceConstants.SHOW_TRADITIONAL_STYLE_TABS, false);
		PlatformUI.getPreferenceStore().setValue(
				IWorkbenchPreferenceConstants.DOCK_PERSPECTIVE_BAR, IWorkbenchPreferenceConstants.TOP_RIGHT);
		PlatformUI.getPreferenceStore().setValue(
				IWorkbenchPreferenceConstants.SHOW_MEMORY_MONITOR, true);
/*		PlatformUI.getPreferenceStore().setValue(
				IWorkbenchPreferenceConstants.SHOW_PROGRESS_ON_STARTUP, true);
*/	
		

		// Display all Perspectives by default
		PlatformUI.getPreferenceStore().setValue(
				IWorkbenchPreferenceConstants.PERSPECTIVE_BAR_EXTRAS,
				Perspective.ID + ", com.netifera.platform.ui.perspectives.explore, com.netifera.platform.ui.perspectives.sniffing");
    }
    
    @Override
    public void postWindowOpen() {
    	try {
    		// Activate the Console and Tasks views so they can show state change in their icons even if they start in fastview mode
    		// Otherwise we must open the fast view first, and then the icon changes will be shown
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(
					"com.netifera.platform.views.console",
					null,
					IWorkbenchPage.VIEW_CREATE);
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(
					"com.netifera.platform.ui.views.Tasks",
					null,
					IWorkbenchPage.VIEW_CREATE);
		} catch (PartInitException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}    	
    }
    
    /* 
     * Called when the window is closed with the top-right corner X or keyboard shortcut
     */
    @Override
    public boolean preWindowShellClose() {
    	Boolean result = Boolean.FALSE;
    	final IHandlerService handlerService = (IHandlerService) PlatformUI
		.getWorkbench().getAdapter(IHandlerService.class);
    	
    	try {
    		/* event is null, the handler wont call workbench.close() */
    		result = (Boolean) handlerService.executeCommand("com.netifera.platform.ui.application.closeWorkspace", null);
		} catch (Exception e) {
			//XXX log something here
		}
		return result;
    }
}
