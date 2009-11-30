package com.netifera.platform.ui.console;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.part.ViewPart;

public class ConsoleView extends ViewPart {

	final private static String CONSOLE_ICON = "icons/console.png";
	final private static String CONSOLE_ALERT_ICON = "icons/console_alert.png";
	
	final private static int ALERT_TIME = 4000;
	
	private StyledText output;
	private ConsoleLogReader reader;
	private MenuManager contextMenu;
	
	private long lastAlertTime = System.currentTimeMillis();
	private boolean alertState = false;
	
	
	@Override
	public void createPartControl(Composite parent) {
		output = new StyledText(parent, SWT.READ_ONLY | SWT.V_SCROLL | SWT.H_SCROLL);

		output.setFont(JFaceResources.getTextFont());
		reader = new ConsoleLogReader(this);
		Activator.getDefault().getLogManager().setLogReader(reader);
		
		/* create and set context menu */
		contextMenu = new MenuManager("#PopupMenu");
		fillContextMenu(contextMenu);
		output.setMenu(contextMenu.createContextMenu(output));
	}

	@Override
	public void setFocus() {
	}

	public void addOutput(final String message) {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				if(output.isDisposed()) 
					return;
				output.append(message);
				output.setCaretOffset(output.getCharCount());
				output.showSelection();
				
				showAlert();
			}
		});
	}

	private void fillContextMenu(IMenuManager menuMgr) {
		menuMgr.add(new Action("Clear"){
			public void run() {
				output.setText("");
			}
		});
		
		/* add standard separator to handle additions */
		menuMgr.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	private void showAlert() {
		lastAlertTime = System.currentTimeMillis();
		if (!alertState) {
			alertState = true;
			setTitleImage(Activator.getDefault().getImageCache().get(CONSOLE_ALERT_ICON));
			Display.getDefault().timerExec(ALERT_TIME, new Runnable() {
				public void run() {
					if (lastAlertTime + ALERT_TIME <= System.currentTimeMillis()) {
						alertState = false;
						setTitleImage(Activator.getDefault().getImageCache().get(CONSOLE_ICON));
					} else {
						Display.getDefault().timerExec(ALERT_TIME, this);
					}
				}
			});
		}
	}
}
