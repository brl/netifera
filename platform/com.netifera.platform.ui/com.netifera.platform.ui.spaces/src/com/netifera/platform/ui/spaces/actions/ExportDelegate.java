package com.netifera.platform.ui.spaces.actions;

import java.io.FileWriter;
import java.io.IOException;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;

import com.netifera.platform.api.model.ISpace;
import com.netifera.platform.ui.internal.spaces.Activator;
import com.netifera.platform.ui.spaces.SpaceEditorInput;

public class ExportDelegate implements IEditorActionDelegate {

	private IEditorPart targetEditor;
	
	public void run(IAction action) {
		IEditorInput input = targetEditor.getEditorInput();
		if(input instanceof SpaceEditorInput)  {
			ISpace space = ((SpaceEditorInput)input).getSpace();
				
			FileDialog dialog = new FileDialog(targetEditor.getEditorSite().getShell(), SWT.SAVE);
			dialog.setText("Export Space to XML");
			dialog.setFilterExtensions(new String[] {"xml"});
			dialog.setOverwrite(true);
			dialog.setFileName(space.getName()+".xml");
			
			String path = dialog.open();
			if(path != null) {
				try {
					FileWriter writer = new FileWriter(path);
					Activator.getInstance().getImportExportService().exportEntities(space.getEntities(), writer);
					writer.close();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	public void selectionChanged(IAction action, ISelection selection) {
	}
	
	public void setActiveEditor(IAction action, IEditorPart targetEditor) {
		this.targetEditor = targetEditor;
	}
}
