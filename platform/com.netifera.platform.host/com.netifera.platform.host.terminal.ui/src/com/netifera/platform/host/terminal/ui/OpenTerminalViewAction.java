package com.netifera.platform.host.terminal.ui;

import java.net.URI;

import org.eclipse.tm.internal.terminal.connector.TerminalConnector;
import org.eclipse.tm.internal.terminal.provisional.api.provider.TerminalConnectorImpl;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.netifera.platform.host.internal.terminal.ui.Activator;
import com.netifera.platform.host.internal.terminal.ui.pty.PTYConnector;
import com.netifera.platform.host.terminal.ITerminalService;
import com.netifera.platform.host.terminal.ui.view.TerminalView;
import com.netifera.platform.ui.actions.SpaceAction;

public abstract class OpenTerminalViewAction extends SpaceAction {

	public OpenTerminalViewAction(String name) {
		super(name);
		setImageDescriptor(Activator.getInstance().getImageCache().getDescriptor("icons/terminal_view.png"));
	}

	public abstract URI getTerminalURL();
	
	public void run() {
		
		final ITerminalService terminalService = (ITerminalService) Activator.getInstance().getServiceFactory().create(ITerminalService.class, getTerminalURL(), getSpace().getProbeId());
		
		try {
			IViewPart view = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView("org.eclipse.tm.terminal.view.TerminalView", 
					"SecondaryTerminal" + System.currentTimeMillis(), IWorkbenchPage.VIEW_ACTIVATE);
			TerminalView terminalView = (TerminalView) view;
	
			TerminalConnector.Factory factory = new TerminalConnector.Factory() {
				public TerminalConnectorImpl makeConnector() throws Exception {
					return new PTYConnector(terminalService);
				}
			};
			terminalView.setConnector(new TerminalConnector(factory, "pty-terminal", "Terminal"));
			terminalView.setSpace(getSpace());
			terminalView.onTerminalConnect();
		} catch (PartInitException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
}
