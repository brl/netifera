package com.netifera.platform.ui.spaces.hover;

import org.eclipse.jface.wizard.Wizard;

import com.netifera.platform.ui.api.actions.ISpaceAction;

public class RunActionWizard extends Wizard {

	private RunActionOptionsPage firstPage;
	private ISpaceAction action;
	
	public RunActionWizard(ISpaceAction action) {
		this.action = action;
	}
	
	@Override
	public void addPages() {
		setWindowTitle(action.getText());
		
//		ImageDescriptor image = Activator.getInstance().getImageCache().getDescriptor("icons/new_probe_wiz.png");
		
		firstPage = new RunActionOptionsPage(action);
//		firstPage.setImageDescriptor(image);
		
		addPage(firstPage);
	}
	
	@Override
	public boolean performFinish() {
		return true;
	}
}
