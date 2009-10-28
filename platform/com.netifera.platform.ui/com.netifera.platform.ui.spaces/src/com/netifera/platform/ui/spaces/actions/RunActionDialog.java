package com.netifera.platform.ui.spaces.actions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.forms.HyperlinkSettings;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;

import com.netifera.platform.api.tools.IOption;
import com.netifera.platform.tools.options.BooleanOption;
import com.netifera.platform.tools.options.GenericOption;
import com.netifera.platform.tools.options.IntegerOption;
import com.netifera.platform.tools.options.IterableOption;
import com.netifera.platform.tools.options.MultipleStringOption;
import com.netifera.platform.tools.options.Option;
import com.netifera.platform.tools.options.StringOption;
import com.netifera.platform.ui.api.actions.ISpaceAction;
import com.netifera.platform.ui.internal.spaces.Activator;
import com.netifera.platform.ui.spaces.actions.options.BooleanOptionWidget;
import com.netifera.platform.ui.spaces.actions.options.GenericOptionWidget;
import com.netifera.platform.ui.spaces.actions.options.IntegerOptionWidget;
import com.netifera.platform.ui.spaces.actions.options.IterableOptionWidget;
import com.netifera.platform.ui.spaces.actions.options.MultipleStringOptionWidget;
import com.netifera.platform.ui.spaces.actions.options.OptionWidget;
import com.netifera.platform.ui.spaces.actions.options.StringOptionWidget;

public class RunActionDialog extends PopupDialog {
	
	private FormToolkit toolkit;
	private ScrolledForm form;
	private Composite body;

	private final ISpaceAction action;
	private final List<OptionWidget> widgets = new ArrayList<OptionWidget>();
	
	private Image runImage = Activator.getDefault().getImageCache().get("icons/run_exc_16x16.png");

	private ImageHyperlink runLink;
	
	
	public RunActionDialog(Shell parent, Point location, ISpaceAction action) {
		super(parent, PopupDialog.INFOPOPUP_SHELLSTYLE | SWT.ON_TOP, true, false, false, false, false, /*action.getText()*/ null, "Press ESC to exit");
		this.action = action;
		
		create();
		getShell().setLocation(location);

		setHeader();
		addOptions();
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);

		composite.setLayout(new FillLayout());
		
		toolkit = new FormToolkit(composite.getDisplay());
		form = toolkit.createScrolledForm(composite);
		
		FormColors colors = toolkit.getColors();
		colors.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_INFO_BACKGROUND));

		toolkit.getHyperlinkGroup().setHyperlinkUnderlineMode(HyperlinkSettings.UNDERLINE_HOVER);
		
//		toolkit.getHyperlinkGroup().setActiveForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
//		toolkit.getHyperlinkGroup().setForeground(colors.getColor("Categorytitle"));
		toolkit.getHyperlinkGroup().setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
		
		body = form.getBody();
		GridLayout bodyLayout = new GridLayout();
		bodyLayout.verticalSpacing = 3;
		body.setLayout(bodyLayout);
//		body.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
		toolkit.paintBordersFor(body);
		
		return composite;
	}

	private void setHeader() {
		form.setFont(JFaceResources.getDialogFont());
		form.setImage(EntityHover.getActionImage(action));
		form.setText(action.getText());
//		form.setSeparatorVisible(true);
//		toolkit.decorateFormHeading(form);
	}

	private void updateRunButton() {
		for (OptionWidget widget: widgets)
			if (!widget.isValid()) {
				runLink.setVisible(false);
				runLink.setEnabled(false);
				return;
			}
		runLink.setEnabled(true);
		runLink.setVisible(true);
	}
	
	private void addOption(Composite parent, IOption option) {
		if(option instanceof StringOption) {
			StringOptionWidget widget = new StringOptionWidget(parent, toolkit, (StringOption)option) {
				protected void accept() {
					safeRun();
				}
				protected void modified() {
					updateRunButton();
				}
			};
			widgets.add(widget);
		} else if(option instanceof IntegerOption) {
			IntegerOptionWidget widget = new IntegerOptionWidget(parent, toolkit, (IntegerOption)option) {
				protected void accept() {
					safeRun();
				}
				protected void modified() {
					updateRunButton();
				}
			};
			widgets.add(widget);
		} else if(option instanceof BooleanOption) {
			widgets.add(new BooleanOptionWidget(parent, toolkit, (BooleanOption)option));
		} else if(option instanceof GenericOption) {
			widgets.add(new GenericOptionWidget(parent, toolkit, (GenericOption)option, action.getSpace()));
		} else if(option instanceof IterableOption) {
			widgets.add(new IterableOptionWidget(parent, toolkit, (IterableOption)option));
		} else if(option instanceof MultipleStringOption) {
			widgets.add(new MultipleStringOptionWidget(parent, toolkit, (MultipleStringOption)option));
		} else {
			//FIXME
			System.err.println("UI cannot handle option "+option);
		}
	}
	
	private void addOptions() {
		Map<String, Composite> sections = new HashMap<String, Composite>();
		
		for(IOption option : action.getConfiguration().getOptions()) {
			Composite parent = body;
			if (option instanceof Option) {
				if (((Option)option).isFixed())
					continue;
				String sectionName = ((Option)option).getSection();
				if (sectionName != null) {
					parent = sections.get(sectionName);
					if (parent == null) {
						Section section = toolkit.createSection(body, Section.TITLE_BAR|Section.TWISTIE|Section.EXPANDED);
						section.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
						section.setExpanded(false);
						section.addExpansionListener(new ExpansionAdapter() {
							public void expansionStateChanged(ExpansionEvent e) {
								form.reflow(true);
							}
						});
						section.setText(sectionName);
//						section.setDescription("This is the description that goes below the title");
						Composite sectionClient = toolkit.createComposite(section);
						sectionClient.setLayout(new GridLayout());
						section.setClient(sectionClient);
						sections.put(sectionName, sectionClient);
						parent = sectionClient;
					}
				}
			}
			addOption(parent, option);
		}

		runLink = toolkit.createImageHyperlink(body, SWT.NONE);
//		runLink.setFont(JFaceResources.getHeaderFont());
		runLink.setImage(runImage);
		runLink.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, true));
		runLink.setText("Run");
		updateRunButton();

		runLink.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				safeRun();
			}
		});
	}

	private void safeRun() {
		if (!runLink.isEnabled())
			return;
		
		try {
			doRun();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	private void doRun() {
		for(OptionWidget optionWidget : widgets)
			optionWidget.setOptionValue();
		
		action.run();
		close();
	}
	
	@Override
	protected void adjustBounds() {
		getShell().pack();
//		Point size = getShell().getSize();
//		size.x = ToolPanel.PANEL_PIXEL_WIDTH;
//		getShell().setSize(size);
	}
	
	@Override
	protected Control getFocusControl() {
		return runLink;
	}

	@Override
	protected void handleShellCloseEvent() {
		super.handleShellCloseEvent();
		toolkit.dispose();
	}
}