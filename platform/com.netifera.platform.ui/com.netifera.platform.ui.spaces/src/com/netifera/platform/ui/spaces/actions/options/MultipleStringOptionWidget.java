package com.netifera.platform.ui.spaces.actions.options;

import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.forms.widgets.FormToolkit;

import com.netifera.platform.tools.options.MultipleStringOption;

public class MultipleStringOptionWidget extends OptionWidget {

	private CheckboxTableViewer viewer;
	
	public MultipleStringOptionWidget(Composite parent, FormToolkit toolkit, final MultipleStringOption option) {
		super(parent, toolkit, option);
		
		Composite area = toolkit.createComposite(parent);
		area.setLayout(new GridLayout(1,false));
		area.setToolTipText(option.getDescription());
		
		Label label = toolkit.createLabel(area, option.getLabel());
		label.setToolTipText(option.getDescription());

		Table table = new Table(area, SWT.CHECK | SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
		table.setHeaderVisible(false);
		table.setLinesVisible(true);

		table.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
		GridData gd = new GridData(SWT.BEGINNING, SWT.FILL, true, false);
		gd.widthHint = 200;
		table.setLayoutData(gd);
		table.setToolTipText(option.getDescription());

		TableColumn col1 = new TableColumn(table, SWT.NONE);
//		col1.setText("Name");
//		TableColumn col2 = new TableColumn(table, SWT.NONE);
//		col2.setText("Size");

		TableLayout layout = new TableLayout();
		layout.addColumnData(new ColumnWeightData(33));
		layout.addColumnData(new ColumnWeightData(33));
		table.setLayout(layout);

		viewer = new CheckboxTableViewer(table);
		viewer.setContentProvider(new IStructuredContentProvider() {
			MultipleStringOption option;
			public Object[] getElements(Object inputElement) {
				return option == null ? new String[0] : option.getPossibleValues();
			}
			public void dispose() {
				// TODO Auto-generated method stub
			}
			public void inputChanged(Viewer viewer, Object oldInput,
					Object newInput) {
				option = (MultipleStringOption) newInput;
			}
		});
		viewer.setLabelProvider(new ITableLabelProvider() {
			public Image getColumnImage(Object element, int columnIndex) {
				// TODO Auto-generated method stub
				return null;
			}
			public String getColumnText(Object element, int columnIndex) {
				return element.toString();
			}
			public void addListener(ILabelProviderListener listener) {
				// TODO Auto-generated method stub
			}
			public void dispose() {
				// TODO Auto-generated method stub
			}
			public boolean isLabelProperty(Object element, String property) {
				// TODO Auto-generated method stub
				return false;
			}
			public void removeListener(ILabelProviderListener listener) {
				// TODO Auto-generated method stub
			}
		});
		
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection sel = (IStructuredSelection) event.getSelection();
//				System.out.println(sel.size() + " items selected, " + viewer.getCheckedElements().length + " items checked");
				setOptionValue();
				modified();
			}
		});
		viewer.setInput(option);
		
		toolkit.paintBordersFor(area);

		if (option.isFixed())
			table.setEnabled(false);
	}
	
	public MultipleStringOption getOption() {
		return (MultipleStringOption) super.getOption();
	}

	public boolean isValid() {
		return true;
	}
	
	public void setOptionValue() {
//		if (index < 0) {
//			getOption().setToDefault();
//		} else {
			Object[] checkedElements = viewer.getCheckedElements();
			String[] checkedStrings = new String[checkedElements.length];
			for (int i=0; i<checkedElements.length; i++)
				checkedStrings[i] = (String) checkedElements[i];
			getOption().setValue(checkedStrings);
//		}
	}
}
