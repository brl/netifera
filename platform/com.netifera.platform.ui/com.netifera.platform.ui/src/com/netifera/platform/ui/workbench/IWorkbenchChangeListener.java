package com.netifera.platform.ui.workbench;

import org.eclipse.ui.IWorkbenchPage;

public interface IWorkbenchChangeListener {
	void perspectiveActivated(String id);
	void partChange();
	void activePageOpened(IWorkbenchPage page);
}
