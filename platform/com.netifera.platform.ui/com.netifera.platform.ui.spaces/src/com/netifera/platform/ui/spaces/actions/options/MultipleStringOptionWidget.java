package com.netifera.platform.ui.spaces.actions.options;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.FormToolkit;

import com.netifera.platform.tools.options.MultipleStringOption;

public class MultipleStringOptionWidget extends OptionWidget {

	private List<Button> buttons;
	
	public MultipleStringOptionWidget(Composite parent, FormToolkit toolkit, final MultipleStringOption option) {
		super(parent, toolkit, option);
		
		Composite area = toolkit.createComposite(parent);
		area.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
		GridLayout layout = new GridLayout(1,false);
		layout.verticalSpacing = 0;
		area.setLayout(layout);
		area.setToolTipText(option.getDescription());
		
		Label label = toolkit.createLabel(area, option.getLabel());
		label.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, false, false));
		label.setToolTipText(option.getDescription());

		buttons = new ArrayList<Button>();
		for (String value: option.getPossibleValues()) {
			Button button = toolkit.createButton(area, value, SWT.CHECK);
			button.setSelection(false);
			button.addSelectionListener(new SelectionListener() {
				public void widgetDefaultSelected(SelectionEvent arg0) {
					setOptionValue();
					modified();
				}
				public void widgetSelected(SelectionEvent arg0) {
					setOptionValue();
					modified();
				}
			});
			
			if (option.isFixed())
				button.setGrayed(true);
			
			buttons.add(button);
		}
				
		toolkit.paintBordersFor(area);
	}
	
	public MultipleStringOption getOption() {
		return (MultipleStringOption) super.getOption();
	}

	public boolean isValid() {
		return true;
	}
	
	public void setOptionValue() {
		List<String> checkedStrings = new ArrayList<String>();
		for (Button button: buttons)
			if (button.getSelection())
				checkedStrings.add(button.getText());
		getOption().setValue(checkedStrings.toArray(new String[checkedStrings.size()]));
	}
}
