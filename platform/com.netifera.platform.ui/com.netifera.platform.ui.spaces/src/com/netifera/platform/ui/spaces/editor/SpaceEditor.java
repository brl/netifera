package com.netifera.platform.ui.spaces.editor;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.viewers.ContentViewer;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableEditor;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

import com.netifera.platform.api.events.IEvent;
import com.netifera.platform.api.events.IEventHandler;
import com.netifera.platform.api.model.IEntity;
import com.netifera.platform.api.model.ISpace;
import com.netifera.platform.api.model.ISpaceTaskChangeEvent;
import com.netifera.platform.api.model.SpaceNameChangeEvent;
import com.netifera.platform.ui.internal.spaces.Activator;
import com.netifera.platform.ui.spaces.ISpaceEditor;
import com.netifera.platform.ui.spaces.SpaceEditorInput;
import com.netifera.platform.ui.spaces.editor.actions.ChangeVisualizationAction;
import com.netifera.platform.ui.spaces.visualizations.ISpaceVisualization;
import com.netifera.platform.ui.util.SelectionProviderProxy;

public class SpaceEditor extends EditorPart implements IPersistableEditor, ISpaceEditor {
	public final static String ID = "com.netifera.platform.editors.spaces";

	private ISpace space;
	private String visualizationName = "Tree";
	private ContentViewer viewer;
	private ToolBar toolBar;
	private ISpaceVisualization currentVisualization;
	
	@Override
	public void doSave(IProgressMonitor monitor) {
	}

	@Override
	public void doSaveAs() {
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		if(!(input instanceof SpaceEditorInput)) {
			throw new PartInitException("SpaceEditor passed unexpected input type");
		}
		
		setSite(site);
		setInput(input);
		setPartName(input.getName());
		setTitleImage();
		
		space = ((SpaceEditorInput)input).getSpace();
		space.addChangeListener(new IEventHandler() {
			public void handleEvent(final IEvent event) {
				if(event instanceof SpaceNameChangeEvent) {
					getSite().getShell().getDisplay().syncExec(new Runnable() {
						public void run() {
							setPartName(((SpaceNameChangeEvent)event).getName());
						}
					});
				}
			}
		});
		
		space.addTaskChangeListener(new IEventHandler() {
			public void handleEvent(final IEvent event) {
				if(event instanceof ISpaceTaskChangeEvent) {
					ISpaceTaskChangeEvent taskEvent = (ISpaceTaskChangeEvent) event;
					if (taskEvent.isCreationEvent() || taskEvent.isUpdateEvent() && !taskEvent.getTask().isRunning()) {
						//FIXME NPE
						getSite().getShell().getDisplay().asyncExec(new Runnable() {
							public void run() {
								setTitleImage();
							}
						});
					}
				}
			}
		});
		
		getSite().getWorkbenchWindow().getSelectionService().addPostSelectionListener(
				new ISelectionListener() {
					public void selectionChanged(IWorkbenchPart part, org.eclipse.jface.viewers.ISelection sel) {
						if (part == SpaceEditor.this)
							return;
						if(sel instanceof IStructuredSelection && !sel.isEmpty()) {
							Object o = ((IStructuredSelection)sel).iterator().next();
							if(o instanceof IEntity) {
								focusEntity((IEntity)o);
							}
						}
					}
				});
	}
	
	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void createPartControl(Composite parent) {
//		IToolBarManager contributions = getEditorSite().getActionBars().getToolBarManager();
//		contributions.removeAll();
		FormLayout layout = new FormLayout();
		parent.setLayout(layout);
		
		toolBar = new ToolBar(parent, SWT.BORDER);
		FormData formData = new FormData();
//		formData.top = new FormAttachment(0,0);
//		formData.bottom = new FormAttachment(100,0);
		formData.left = new FormAttachment(0,0);
		formData.right = new FormAttachment(100,0);
		toolBar.setLayoutData(formData);

		IToolBarManager contributions = new ToolBarManager(toolBar);
		contributions.add(new ChangeVisualizationAction(this));
		currentVisualization = Activator.getInstance().getVisualizationFactory().create(visualizationName, space);
		viewer = currentVisualization.createViewer(parent);
		
		/* set the visualization provide viewer as selection provider*/
		setSelectionProvider(viewer);
		
		currentVisualization.addContributions(contributions);
		
		formData = new FormData();
		formData.top = new FormAttachment(toolBar,0);
		formData.bottom = new FormAttachment(100,0);
		formData.left = new FormAttachment(0,0);
		formData.right = new FormAttachment(100,0);
		viewer.getControl().setLayoutData(formData);

		contributions.update(true);
	}
	
	private void setSelectionProvider(ISelectionProvider selectionProvider) {
		ISelectionProvider currentProvider = getSite().getSelectionProvider();
		if(currentProvider == null) {
			currentProvider = new SelectionProviderProxy();
			getSite().setSelectionProvider(currentProvider);
		}
		
		if(currentProvider instanceof SelectionProviderProxy) {
			((SelectionProviderProxy) currentProvider).setSelectionProvider(viewer);
		}
	}
	
	private void setTitleImage() {
		setTitleImage(Activator.getInstance().getImageCache().get(getEditorInput().getImageDescriptor()));
	}
	
	public String getVisualization() {
		return visualizationName;
	}
	
	public void setVisualization(String name) {
		Composite parent = viewer.getControl().getParent();
		viewer.getControl().dispose();
		toolBar.dispose();
		visualizationName = name;
		createPartControl(parent);
		parent.layout();
	}

	public void dispose() {
		//FIXME remove space listeners
		super.dispose();
		space.close(); //FIXME what if two editors in the same space?
	}

	@Override
	public void setFocus() {
		viewer.getControl().setFocus();
		setSelectionProvider(viewer);
	}

	public void restoreState(IMemento memento) {
		visualizationName = memento.getString("visualization");
		if (visualizationName == null) visualizationName = "Tree";
	}

	public void saveState(IMemento memento) {
		memento.putString("visualization", visualizationName);
	}

	public void focusEntity(IEntity entity) {
		if(currentVisualization != null)
			currentVisualization.focusEntity(entity);
	}
}
