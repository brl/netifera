package com.netifera.platform.ui.tasks.output;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

import com.netifera.platform.api.tasks.ITaskRecord;
import com.netifera.platform.ui.util.ViewerRefreshAction;

public class TaskOutputView extends ViewPart {
	private static final String TASK_VIEW = "com.netifera.platform.ui.views.Tasks";
	public static final String ID = "com.netifera.platform.ui.views.TaskOutput";
	private TaskOutputTableViewer viewer;
	private ITaskRecord record;
	private ISelectionListener selectionListener;

	@Override
	public void createPartControl(Composite parent) {
		/** create a composite control */
		parent.setLayout(new FillLayout());
		viewer = new TaskOutputTableViewer(parent, parent.getStyle(), true, this);
		initializeToolBar();
		parent.layout(true);
	}

	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);
	}

	@Override
	public void setFocus() {
		viewer.getControl().setFocus();
	}

	public void dipose() {
		super.dispose();
		viewer = null;
	}

	private void initializeToolBar() {
		IToolBarManager toolBarManager = getViewSite().getActionBars().getToolBarManager();
		toolBarManager.add(new ViewerRefreshAction(viewer));
	}

	public void addSelectionListener() {
		
		removeSelectionListener();
		/* listener to show the output of the selected task */
		selectionListener = new ISelectionListener() {
			public void selectionChanged(IWorkbenchPart part, org.eclipse.jface.viewers.ISelection sel) {
				if(sel instanceof IStructuredSelection && !sel.isEmpty()) {
					Object o = ((IStructuredSelection)sel).iterator().next();
					setInput(o);
				}
			}
		};
		/* set the current selection */
		selectionListener.selectionChanged(null, 
				getSite().getWorkbenchWindow().getSelectionService().getSelection(TASK_VIEW));
		
		/* add listener task view */
		getSite().getWorkbenchWindow().getSelectionService().addPostSelectionListener(
				TASK_VIEW,selectionListener);
	}
	
	public void removeSelectionListener() {
		if(selectionListener != null) {
			getSite().getWorkbenchWindow().getSelectionService().removePostSelectionListener(selectionListener);
		}
		selectionListener = null;
	}

	public void setInput(Object input) {
		if (input instanceof ITaskRecord) {
			record = (ITaskRecord)input;
			if (record.getTitle() != null)
				setPartName(record.getTitle());
			if (record.getStateDescription() != null)
				setContentDescription(record.getStateDescription());
			viewer.setInput(input);
		}
	}

	public void saveState(IMemento memento) {
		super.saveState(memento);
		long probeId = record.getProbeId();
		long taskId = record.getTaskId();
		memento.putString("probeId", ""+probeId);
		memento.putString("taskId", ""+taskId);
	}
}
