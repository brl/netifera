package com.netifera.platform.ui.navigator;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

import com.netifera.platform.api.events.IEvent;
import com.netifera.platform.api.events.IEventHandler;
import com.netifera.platform.api.model.ISpace;
import com.netifera.platform.api.probe.IProbe;
import com.netifera.platform.ui.internal.navigator.Activator;
import com.netifera.platform.ui.probe.actions.ConnectProbeAction;
import com.netifera.platform.ui.probe.actions.DisconnectProbeAction;
import com.netifera.platform.ui.probe.actions.NewProbeAction;
import com.netifera.platform.ui.spaces.SpaceEditorInput;
import com.netifera.platform.ui.spaces.actions.NewIsolatedSpaceAction;
import com.netifera.platform.ui.spaces.actions.NewSpaceAction;
import com.netifera.platform.ui.spaces.actions.RenameSpaceAction;
import com.netifera.platform.ui.spaces.editor.SpaceEditor;
import com.netifera.platform.ui.util.TreeAction;

public class NavigatorView extends ViewPart {

	private TreeViewer viewer;
	
	private Action connectProbeAction;
	private Action disconnectProbeAction;
	private Action newProbeAction;
	private Action newSpaceAction;
	private Action newIsolatedSpaceAction;
	
	public NavigatorView() {
	}

	@Override
	public void createPartControl(Composite parent) {
		viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);

		viewer.setContentProvider(new NavigatorContentProvider());
		viewer.setLabelProvider(new NavigatorLabelProvider());
		viewer.setInput(Activator.getInstance().getModel().getCurrentWorkspace());
		
		//XXX this kind of updates should be done in the content provider
		Activator.getInstance().getModel().getCurrentWorkspace().addSpaceCreationListener(new IEventHandler() {
			public void handleEvent(IEvent event) {
					Display.getDefault().asyncExec(new Runnable() {
						public void run() {
							if(!viewer.getControl().isDisposed()) {
								viewer.refresh();
							}
						}
					});
				}
		});

		viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				IStructuredSelection selection = (IStructuredSelection) event.getSelection();
				if (selection.getFirstElement() instanceof ISpace) {
					handleDoubleClick((ISpace) selection.getFirstElement());
				} else {
					Object element = selection.getFirstElement();
					if (viewer.getExpandedState(element)) {
						viewer.collapseToLevel(element, 1);
					} else {
						viewer.expandToLevel(element, 1);						
					}
				}
			}
		});
		
		Activator.getInstance().getProbeManager().addProbeChangeListener(
				createProbeChangeHandler(parent.getDisplay()));

		initializeContextMenu();
		initializeToolBar();
	}
	
	private IEventHandler createProbeChangeHandler(final Display display) {
		return new IEventHandler() {
			public void handleEvent(IEvent event) {
				display.asyncExec(new Runnable() {
					public void run() {
						viewer.refresh();
						setActionEnableStates();
					}
				});
			}
		};
	}

	private void fillContextMenu(IMenuManager menuMgr) {
		if (getSelectedSpace() != null) {
			menuMgr.add(newSpaceAction);
			menuMgr.add(newIsolatedSpaceAction);
			menuMgr.add(new Separator("fixedGroup"));
			menuMgr.add(new RenameSpaceAction(getSelectedSpace()));
		}
		if (getSelectedProbe() != null) {
			menuMgr.add(newSpaceAction);
			menuMgr.add(newIsolatedSpaceAction);
//			menuMgr.add(newProbeAction);
			menuMgr.add(new Separator("fixedGroup"));
			menuMgr.add(connectProbeAction);
			menuMgr.add(disconnectProbeAction);
		}
	}

	private void initializeContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager m) {
				fillContextMenu(m);
			}
		});

		Control viewerControl = viewer.getControl();
		Menu menu = menuMgr.createContextMenu(viewerControl);
		viewerControl.setMenu(menu);
		/* register the pop-up menu using viewer selection provider */
		getSite().registerContextMenu(menuMgr, viewer);
	}
	
	private void handleDoubleClick(ISpace space) {
		try {
			if(space.isOpened()) {
				focusEditorForSpace(space);
			} else {
				openEditorForSpace(space);
			}
		} catch(PartInitException e) {
			
		}
	}
	
	private void openEditorForSpace(ISpace space) throws PartInitException {
		final IEditorInput input = new SpaceEditorInput(space);
		space.open();
		getPage().openEditor(input, SpaceEditor.ID);
	}
	
	private void focusEditorForSpace(ISpace space) throws PartInitException {
		for(IEditorReference reference : getPage().getEditorReferences()) {
			if(reference.getEditorInput() instanceof SpaceEditorInput) {
				SpaceEditorInput input = (SpaceEditorInput) reference.getEditorInput();
				if(input.getSpace() == space) {
					getPage().activate(reference.getEditor(true));
				}
			}
		}
	}
	
	private IWorkbenchPage getPage() {
		return Activator.getInstance().getWorkbench().getActiveWorkbenchWindow().getActivePage();
	}
	
	private void initializeToolBar() {
		final IToolBarManager toolBarManager = getViewSite().getActionBars().getToolBarManager();

		newSpaceAction = new NewSpaceAction(this, viewer);
		toolBarManager.add(newSpaceAction);

		newIsolatedSpaceAction = new NewIsolatedSpaceAction(this, viewer);
		toolBarManager.add(newIsolatedSpaceAction);

		newProbeAction = new NewProbeAction(viewer);
		toolBarManager.add(newProbeAction);

		connectProbeAction = new ConnectProbeAction(viewer);
		toolBarManager.add(connectProbeAction);
		
		disconnectProbeAction = new DisconnectProbeAction(viewer);
		toolBarManager.add(disconnectProbeAction);

		toolBarManager.add(new Separator("fixedGroup"));
		
		toolBarManager.add(TreeAction.collapseAll(viewer));
		toolBarManager.add(TreeAction.expandAll(viewer));

		setActionEnableStates();
		
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				setActionEnableStates();
			}
		});
	}
	
	private void setActionEnableStates() {
		connectProbeAction.setEnabled(getConnectActionState());
		disconnectProbeAction.setEnabled(getDisconnectActionState());
		
		IProbe probe = getSelectedProbe();
		ISpace space = getSelectedSpace();
//		newProbeAction.setEnabled(probe != null);
		newSpaceAction.setEnabled((probe != null && !probe.isLocalProbe()) || (space != null && space.isIsolated()));
		newIsolatedSpaceAction.setEnabled((probe != null && probe.isLocalProbe()) || (space != null && space.isIsolated()));
	}
	
	private boolean getConnectActionState() {
		final IProbe probe = getSelectedProbe();
		if(probe == null) {
			return false;
		}
		
		return probe.isDisconnected();
	}
	
	private boolean getDisconnectActionState() {
		final IProbe probe = getSelectedProbe();
		if(probe == null) 
			return false;
		
		return probe.isConnected() && !probe.isLocalProbe();
	}
	
	private IProbe getSelectedProbe() {
		final Object element = getSelectedObject();
		return element instanceof IProbe ? (IProbe) element : null;
	}

	private ISpace getSelectedSpace() {
		final Object element = getSelectedObject();
		return element instanceof ISpace ? (ISpace) element : null;
	}

	private Object getSelectedObject() {
		final IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
		if(selection == null) 
			return null;
		return selection.getFirstElement();
	}

	@Override
	public void setFocus() {
		viewer.getControl().setFocus();
	}
}
