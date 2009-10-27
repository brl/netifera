package com.netifera.platform.host.processes.ui;

import java.net.URI;

import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.netifera.platform.host.internal.processes.ui.Activator;
import com.netifera.platform.host.processes.IProcessService;
import com.netifera.platform.ui.actions.SpaceAction;

public abstract class OpenProcessListAction extends SpaceAction {
	
	public OpenProcessListAction(final String name) {
		super(name);
		setImageDescriptor(Activator.getInstance().getImageCache().getDescriptor("icons/processes.png"));
	}

	public abstract URI getProcessServiceURL();
	
	@Override
	public void run() {
		IProcessService processService = (IProcessService) Activator.getInstance().getServiceFactory().create(IProcessService.class, getProcessServiceURL(), this.getSpace().getProbeId());
		
		try {
			IViewPart view = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(
					ProcessListView.ID,
					"SecondaryProcessList" + System.currentTimeMillis(),
					IWorkbenchPage.VIEW_ACTIVATE);
			((ProcessListView)view).setInput(processService);
		} catch (PartInitException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
