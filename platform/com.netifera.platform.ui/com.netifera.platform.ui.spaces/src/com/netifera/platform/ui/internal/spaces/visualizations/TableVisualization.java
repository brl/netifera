package com.netifera.platform.ui.internal.spaces.visualizations;

import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.jface.viewers.ContentViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import com.netifera.platform.api.model.AbstractEntity;
import com.netifera.platform.api.model.IEntity;
import com.netifera.platform.api.model.ISpace;
import com.netifera.platform.ui.spaces.hover.ActionHover;
import com.netifera.platform.ui.spaces.table.TableContentProvider;
import com.netifera.platform.ui.spaces.table.TableLabelProvider;
import com.netifera.platform.ui.spaces.visualizations.IVisualization;
import com.netifera.platform.ui.util.HookingViewerComparator;
import com.netifera.platform.ui.util.MouseTracker;

public class TableVisualization implements IVisualization {

	final private ISpace space;
	private TableViewer viewer;

	public TableVisualization(ISpace space) {
		this.space = space;
	}
	
	public void addContributions(IContributionManager contributions) {
		// TODO Auto-generated method stub
	}

	public ContentViewer createViewer(Composite parent) {
		viewer = new TableViewer(parent, SWT.V_SCROLL | SWT.H_SCROLL | SWT.VIRTUAL);
		TableContentProvider contentProvider = new TableContentProvider();
		viewer.setContentProvider(contentProvider);
		TableLabelProvider labelProvider = new TableLabelProvider();
		viewer.setLabelProvider(labelProvider);
		String[] columnNames = new String[] { "Label", "Tags", "Type", "Modification Time" };
		int[] columnWidth = new int[] { 350, 100, 70, 100 };
		int[] columnAlign = new int[] { SWT.LEFT, SWT.LEFT, SWT.LEFT, SWT.LEFT };
		/** get the table widget from the viewer */
		final Table table = viewer.getTable();

		/** define the table columns */
		for (int i = 0; i < columnWidth.length; i++) {
			TableColumn column = new TableColumn(table, columnAlign[i]);
			/* set index as column data */
			column.setData(i);
			column.setText(columnNames[i]);
			column.setWidth(columnWidth[i]);
		}
		
		viewer.setComparator(new HookingViewerComparator(viewer){
			@Override
			public void setAscending(boolean ascending) {
//				contentProvider.setAscending(ascending);
			}
			@Override
			public void setSortBy(Object fieldId) {
				//ignore field now to test concept
			}});
		/* set some table visual properties */
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		
		viewer.setInput(space);

		/* implement the mouse tracker the action hover handlers*/
		final MouseTracker mouseTracker = new MouseTracker(viewer.getTable()) {
			private PopupDialog informationControl;

			@Override
			protected Object getItemAt(Point point) {
				TableItem tableItem = viewer.getTable().getItem(point);
				if (tableItem != null) {
					return tableItem.getData();
				}
				return null;
			}

			protected Rectangle getAreaOfItemAt(Point point) {
				TableItem tableItem = viewer.getTable().getItem(point);
				if (tableItem != null) {
					Rectangle itemArea = tableItem.getBounds();

					/*
					 * the TreeItem getBounds rectangle only includes the text. But
					 * getItem(point) returns the rectangle for any point in the
					 * tree row.
					 */
					return expandedItemArea(itemArea);
				}
				return super.getAreaOfItemAt(point);
			}
		
			private Rectangle expandedItemArea(Rectangle itemArea) {
				return new Rectangle(Math.max(itemArea.x - 12, 2), Math.max(itemArea.y
						- EPSILON * 2, 0), itemArea.width + 12 * 2, itemArea.height
						+ EPSILON * 2 * 2);
			}
			
			@Override
			protected void showInformationControl(Shell parent, Point location,
					Object input, Object item) {
				informationControl = new ActionHover(parent, location, space, item);
				informationControl.open();
			}
			@Override
			protected void hideInformationControl() {
				if(informationControl != null) {
					informationControl.close();
				}
			}
			@Override
			protected boolean focusInformationControl() {
				Shell shell = informationControl.getShell();
				if(shell != null) {
					return shell.setFocus();
				}
				return false;
			}
			@Override
			protected Rectangle getInformationControlArea() {
				if(informationControl != null) {
					Shell shell = informationControl.getShell();
					if(shell != null) {
						return shell.getBounds();
					}
				}
				return null;
			}
		};
		
		viewer.getControl().addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent event) {
				mouseTracker.stop();
			}
		});
		
		return viewer;
	}

	public void focusEntity(IEntity entity) {
		entity = ((AbstractEntity)entity).getRealEntity();
		// FIXME this is inefficient, should use setSelection(int[])
		if (space.contains(entity))
			viewer.setSelection(new StructuredSelection(entity), true);
		else
			viewer.setSelection(new StructuredSelection(), false);
	}
}
