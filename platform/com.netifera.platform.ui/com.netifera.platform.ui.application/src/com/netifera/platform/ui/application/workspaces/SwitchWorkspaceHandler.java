package com.netifera.platform.ui.application.workspaces;


import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.PlatformUI;

public class SwitchWorkspaceHandler extends AbstractHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {
		openChoseWorkspaceDialog(true);
		return null;
	}
	
	public static WorkspaceRecord  openChoseWorkspaceDialog(boolean restart) {
		WorkspaceChooser chooser = new WorkspaceChooser();
		List<WorkspaceRecord> workspaces = chooser.findAllWorkspaces();
		SwitchWorkspaceDialog dialog = new SwitchWorkspaceDialog(null, workspaces);
		WorkspaceRecord workspaceRecord = null;
		
		int dialogButton = dialog.open();

		/* if user clicked "new workspace" open the wizard */
		if(dialogButton == IDialogConstants.NEXT_ID) {
			workspaceRecord = NewWorkspaceHandler.openNewWorkspaceWizard(restart);
			/* if a new workspace is created the wizard restarts the workbench */
		}
		
		else if(dialogButton == Window.OK) {
			workspaceRecord = dialog.getSelectedWorkspaceRecord();

			/* mark the selected workspace to open in next startup */
			if(restart) {
				WorkspaceChooser.markAutostart(workspaceRecord.getPath());
				PlatformUI.getWorkbench().restart();
			}
		}
		return workspaceRecord;
	}	
}
