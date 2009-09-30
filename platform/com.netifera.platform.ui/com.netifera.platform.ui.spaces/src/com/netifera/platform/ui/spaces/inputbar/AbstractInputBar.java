package com.netifera.platform.ui.spaces.inputbar;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.ControlContribution;
import org.eclipse.jface.action.IAction;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorPart;

import com.netifera.platform.api.log.ILogger;
import com.netifera.platform.api.model.ISpace;
import com.netifera.platform.ui.api.actions.ISpaceAction;
import com.netifera.platform.ui.internal.spaces.Activator;
import com.netifera.platform.ui.spaces.SpaceEditorInput;
import com.netifera.platform.ui.spaces.actions.SpaceCreator;
import com.netifera.platform.ui.util.GreyedText;


public abstract class AbstractInputBar extends ControlContribution {
	private final RGB WARNING_COLOR = new RGB(0xF5, 0xA9, 0xA9);
	private String content = "";
	private IAction buttonAction;
	private List<IAction> inputActions = new ArrayList<IAction>();
	private Color warningColor;
	protected Text text;
	protected ILogger logger;

	public AbstractInputBar(String id, ILogger logger) {
		super(id);
		this.logger = logger;
	}

	public void setAction(IAction action) {
		this.buttonAction = action;
	}
	
	public void setEnabled() {
		if(text == null || text.isDisposed()) return;
		
		text.setEnabled(true);
		text.setText("");
		text.setBackground(text.getDisplay().getSystemColor(SWT.COLOR_WHITE));
	}
	
	public void setDisabled(String message) {
		if(text == null || text.isDisposed()) return;
		text.setEnabled(false);
		text.setText(message);
		text.setBackground(warningColor);
	}
	
	@Override
	protected Control createControl(Composite parent) {
		text = new Text(parent, SWT.BORDER | SWT.SINGLE);
		text.setTextLimit(100);
		text.setToolTipText(getDefaultToolTipText());
		text.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				content = text.getText();
				AbstractInputBar.this.doUpdate();
				if (inputActions.size()>0)
					text.setToolTipText(inputActions.get(0).getText());
				else
					if (content.length() == 0)
						text.setToolTipText(getDefaultToolTipText());
					else
						text.setToolTipText(null);
			}
		});
		text.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent e) {
			}
			public void keyReleased(KeyEvent e) {
				if (e.character == SWT.CR) {
					if (AbstractInputBar.this.isActionEnabled()) {
						if ((e.stateMask & SWT.SHIFT) != 0) {
							SpaceCreator creator = new SpaceCreator(Activator.getDefault().getWorkbench().getActiveWorkbenchWindow());
							creator.create(content);
						}
						AbstractInputBar.this.runAction();
						text.setText(content);
						AbstractInputBar.this.update();
					}
				}
			}
		});
		warningColor = new Color(parent.getDisplay(), WARNING_COLOR);
		new GreyedText(text, getDefaultGreyedText());
		return text;
	}

	protected int computeWidth(Control control) {
		return 300;
	}
	
	boolean isActionEnabled() {
		return buttonAction.isEnabled();
	}

	protected void setActionEnabled(boolean enabled) {
		buttonAction.setEnabled(enabled);
	}

	public void runAction() {
		if (!isActionEnabled()) return;
		final ISpace space = getActiveSpace();
		if(space == null) {
			return;
		}
		logger.info("run: "+content+", action="+inputActions.get(0).getText() + " space= " + space.getId());
		IAction action = inputActions.get(0);
	
		if(action instanceof ISpaceAction) {
			((ISpaceAction)action).setSpace(space);
		}
		action.run();

		if (space.getName().matches("Space \\d+")) {
			space.setName(content);
		}

		content = "";
		text.setText(content);
		update();
	}
	
	protected ISpace getActiveSpace() {
		IEditorPart editor = Activator.getDefault().getActiveEditor();
		if(editor == null) {
			return null;
		}
		if(!(editor.getEditorInput() instanceof SpaceEditorInput)) {
			return null;
		}
		return ((SpaceEditorInput)editor.getEditorInput()).getSpace();
	}
	
	private void doUpdate() {
		inputActions = getInputActions(content);
		setActionEnabled(inputActions.size() > 0);
	}
	
	protected abstract String getDefaultToolTipText();
	protected abstract String getDefaultGreyedText();
	protected abstract List<IAction> getInputActions(String content);
}
