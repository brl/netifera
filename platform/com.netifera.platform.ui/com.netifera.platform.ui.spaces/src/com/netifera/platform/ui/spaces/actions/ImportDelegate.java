package com.netifera.platform.ui.spaces.actions;

import java.io.FileReader;
import java.io.IOException;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.netifera.platform.api.model.ISpace;
import com.netifera.platform.ui.internal.spaces.Activator;
import com.netifera.platform.ui.spaces.SpaceEditorInput;
import com.netifera.platform.util.xml.XMLParseException;

public class ImportDelegate implements IWorkbenchWindowActionDelegate {

	private IWorkbenchWindow window;
	
	public void init(IWorkbenchWindow window) {
		this.window = window;
	}

	public void run(IAction action) {
		IEditorPart editor = window.getActivePage().getActiveEditor();
		if(editor != null) {
			IEditorInput input = editor.getEditorInput();
			if(input instanceof SpaceEditorInput)  {
				ISpace space = ((SpaceEditorInput)input).getSpace();
				
				FileDialog dialog = new FileDialog(window.getShell(), SWT.OPEN);
				dialog.setText("Import XML into Space");
				dialog.setFilterExtensions(new String[] {"xml"});
				dialog.setOverwrite(false);
				
				String path = dialog.open();
				if(path != null) {
					try {
						FileReader reader = new FileReader(path);
						Activator.getInstance().getImportExportService().importEntities(space.getRootEntity().getId(), space.getId(), reader);
						reader.close();
					} catch (XMLParseException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	public void selectionChanged(IAction action, ISelection selection) {
	}
	
	public void dispose() {
	}
}
