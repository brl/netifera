package com.netifera.platform.ui.spaces.actions.options;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;

import com.netifera.platform.tools.options.StringOption;

public class StringOptionWidget extends OptionWidget {

	private final Text text;
	private final CCombo combo;
	
	public StringOptionWidget(Composite parent, FormToolkit toolkit, StringOption option) {
		super(parent, toolkit, option);

		Composite area = toolkit.createComposite(parent);
		area.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
		area.setLayout(new GridLayout(2,false));
		area.setToolTipText(option.getDescription());
		
		Label label = toolkit.createLabel(area, option.getLabel());
		label.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, false, false));
		label.setToolTipText(option.getDescription());

		if (option.getPossibleValues() == null) {
			combo = null;
			text = toolkit.createText(area, option.getValue(), SWT.BORDER);
			text.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));
			text.setToolTipText(option.getDescription());
			
			text.addModifyListener(new ModifyListener() {
				public void modifyText(ModifyEvent e) {
					modified();
				}
			});
			text.addKeyListener(new KeyListener() {
				public void keyPressed(KeyEvent e) {
				}
				public void keyReleased(KeyEvent e) {
					modified();
					if (e.character == SWT.CR && isValid())
						accept();
				}
			});
			
			if (option.isFixed())
				text.setEnabled(false);
		} else {
			text = null;
			combo = new CCombo(area, SWT.BORDER|SWT.READ_ONLY);
			combo.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
			combo.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));
			combo.setEditable(false);
			combo.setToolTipText(option.getDescription());
			
			for (String value: option.getPossibleValues()) {
				combo.add(value);
			}
	
			combo.setText(option.getValue());
			
			combo.addModifyListener(new ModifyListener() {
				public void modifyText(ModifyEvent e) {
					modified();
				}	
			});
			combo.addSelectionListener(new SelectionListener() {
				public void widgetDefaultSelected(SelectionEvent e) {
					modified();
				}

				public void widgetSelected(SelectionEvent e) {
					setOptionValue();
					modified();
				}
			});
			
			if (option.isFixed())
				combo.setEnabled(false);
		}
	}
	
	public StringOption getOption() {
		return (StringOption) super.getOption();
	}

	public boolean isValid() {
		return (getOption().allowsEmptyValue() || (getValue().length() > 0)) && isValid(getValue());
	}

	public boolean isValid(String value) {
		return true;
	}

	protected String getValue() {
		return text != null ? text.getText() : combo.getText();
	}
	
	public void setOptionValue() {
		getOption().setValue(getValue());
	}
}
