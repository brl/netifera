package com.netifera.platform.host.filesystem.ui.actions;

import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;

import com.netifera.platform.host.filesystem.File;
import com.netifera.platform.host.filesystem.IFileSystem;
import com.netifera.platform.host.filesystem.ui.FileSystemContentProvider;
import com.netifera.platform.host.internal.filesystem.ui.Activator;

public class CreateDirectoryAction extends AbstractFileSystemAction {

	public CreateDirectoryAction(ISelectionProvider selectionProvider, FileSystemContentProvider contentProvider) {
		super(selectionProvider, contentProvider);
		setText("&New Folder");
		setToolTipText("New Folder");
		ImageDescriptor icon =  Activator.getInstance().getImageCache().getDescriptor("icons/newfolder.png");
		setImageDescriptor(icon);
	}
	
	@Override
	public void run() {
		for (Object o: getSelection().toArray()) {
			if (o instanceof File) {
				File file = (File) o;
				final File parent = file.isDirectory() ? file : file.getParent();
				final IFileSystem fileSystem = parent.getFileSystem();
				InputDialog dialog = new InputDialog(Display.getCurrent().getActiveShell(), "Create New Folder", "Folder Name", "", null);
				if (dialog.open() == Window.OK) {
					final String newFolderName = dialog.getValue();
					new Thread(new Runnable() {
						public void run() {
							try {
								File newFolder = fileSystem.createDirectory(parent.getAbsolutePath() + fileSystem.getNameSeparator() + newFolderName);
								contentProvider.added(newFolder);
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
