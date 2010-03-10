package com.netifera.platform.host.filesystem.ui.actions;

import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;

import com.netifera.platform.host.filesystem.File;
import com.netifera.platform.host.filesystem.ui.FileSystemContentProvider;
import com.netifera.platform.host.internal.filesystem.ui.Activator;

public class RenameAction extends AbstractFileSystemAction {

	public RenameAction(ISelectionProvider selectionProvider, FileSystemContentProvider contentProvider) {
		super(selectionProvider, contentProvider);
		setText("&Rename");
		setToolTipText("Rename");
		ImageDescriptor icon =  Activator.getInstance().getImageCache().getDescriptor("icons/rename.png");
		setImageDescriptor(icon);
	}
	
	@Override
	public void run() {
		for (Object o: getSelection().toArray()) {
			if (o instanceof File) {
				final File file = (File) o;
				InputDialog dialog = new InputDialog(Display.getCurrent().getActiveShell(), "Rename "+file, "New Name", "", null);
				if (dialog.open() == Window.OK) {
					final String newName = dialog.getValue();
					new Thread(new Runnable() {
						public void run() {
							try {
								contentProvider.removed(file);
								if (!file.renameTo(file.getParent().getAbsolutePath()+file.getFileSystem().getNameSeparator()+newName))
									Activator.getInstance().getBalloonManager().error("Rename failed");
								contentProvider.added(file);
							} catch (Exception e) {
								Activator.getInstance().getBalloonManager().error(e.toString());
							}
						}
					}).start();
				}
			}
		}
	}
}
