package com.netifera.platform.ui.spaces.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;

public class NewSpaceDelegate implements IEditorActionDelegate {

	private IEditorPart targetEditor;

	public void run(IAction action) {
		SpaceCreator creator = new SpaceCreator(targetEditor.getEditorSite().getWorkbenchWindow());
		creator.openNewSpace();
	}

	public void selectionChanged(IAction action, ISelection selection) {
	}
	
	public void setActiveEditor(IAction action, IEditorPart targetEditor) {
		this.targetEditor = targetEditor;
	}
}
