package com.netifera.platform.ui.application.workspaces;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.Wizard;

import com.netifera.platform.ui.application.ApplicationPlugin;

public class NewWorkspaceWizard extends Wizard {
	private WorkspaceNamePage firstPage;
	private LastPage lastPage;
	private WorkspaceRecord workspaceRecord;
	private boolean restart = true;
	
	public void addPages() {
		setWindowTitle("Create a new Workspace");
		
		ImageDescriptor image = ApplicationPlugin.getImageDescriptor("icons/workspace_wiz.gif");

		firstPage = new WorkspaceNamePage();
		firstPage.setImageDescriptor(image);
		
		lastPage = new LastPage();
		lastPage.setImageDescriptor(image);
		
		addPage(firstPage);
		addPage(lastPage);
	}
	
	public boolean canFinish() {
		return getContainer().getCurrentPage() == lastPage;
	}
	
	@Override
	public boolean performFinish() {
		final String name = firstPage.getWorkspaceName();
		setWorkspaceRecord(WorkspaceChooser.createWorkspace(name, isRestart()));
		return true;
	}

	public void setWorkspaceRecord(WorkspaceRecord workspaceRecord) {
		this.workspaceRecord = workspaceRecord;
	}

	public WorkspaceRecord getWorkspaceRecord() {
		return workspaceRecord;
	}

	public void setRestart(boolean restart) {
		this.restart = restart;
	}

	public boolean isRestart() {
		return restart;
	}
}
