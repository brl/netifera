package com.netifera.platform.net.http.internal.ui.actions;

import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

import com.netifera.platform.api.model.IEntity;
import com.netifera.platform.net.http.internal.spider.daemon.remote.WebSpiderConfiguration;
import com.netifera.platform.net.http.internal.ui.Activator;
import com.netifera.platform.net.http.service.HTTP;
import com.netifera.platform.net.http.spider.daemon.IWebSpiderDaemon;
import com.netifera.platform.net.http.spider.impl.WebSite;
import com.netifera.platform.net.http.web.model.WebSiteEntity;

public class ConfigPanel extends PopupDialog {

	private FormToolkit toolkit;
	private Form form;
	private Composite body;
	private IWebSpiderDaemon daemon;
	private final WebSpiderActionManager manager;

	public ConfigPanel(Shell parent, WebSpiderActionManager manager) {
		super(parent, PopupDialog.INFOPOPUP_SHELLSTYLE | SWT.ON_TOP, true, false, false, false, false, null, "Press 'ESC' to exit");
		daemon = Activator.getDefault().getWebSpiderDaemon();
		this.manager = manager;
		create();
		setHeader();
		addOptionsSection();
		addModulesSection();
		addTargetsSection();
	}
	
	protected Point getInitialLocation(Point initialSize) {
		return manager.getConfigDialogLocation();
	}
	
	public int open() {
		manager.disableAll();
		return super.open();
	}
	
	public boolean close() {
		manager.setState();
		return super.close();
	}
	
	
	protected Control createDialogArea(Composite parent) {
		final Composite composite = (Composite) super.createDialogArea(parent);
		
		composite.setLayout(new FillLayout());
		toolkit = new FormToolkit(composite.getDisplay());
		form = toolkit.createForm(composite);
		
		FormColors colors = toolkit.getColors();
		colors.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
		body = form.getBody();
		body.setLayout(new GridLayout());
		toolkit.paintBordersFor(body);
		return composite;
	}
	
	private void setHeader() {
		form.setFont(JFaceResources.getDefaultFont());
		form.setText("Configure Web Spider");
		form.setSeparatorVisible(true);
		toolkit.decorateFormHeading(form);
	}
	
	private void addOptionsSection() {
		Section section = toolkit.createSection(form.getBody(), Section.DESCRIPTION |
				Section.TITLE_BAR | Section.TWISTIE | Section.EXPANDED);
		section.setText("Web Spider Options");
		section.setDescription("Set options for the web spider");
		
		Composite sectionClient = toolkit.createComposite(section);
		sectionClient.setLayout(new GridLayout());
		section.setClient(sectionClient);

		final WebSpiderConfiguration config = daemon.getConfiguration();
		final Button b1 = toolkit.createButton(sectionClient, "Follow Links", SWT.CHECK);
		b1.setSelection(config.followLinks);
		b1.setEnabled(true);
		b1.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				config.followLinks = b1.getSelection();
				daemon.setConfiguration(config);
			}
		});

		final Button b2 = toolkit.createButton(sectionClient, "Fetch Images", SWT.CHECK);
		b2.setSelection(config.fetchImages);
		b2.setEnabled(true);
		b2.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				config.fetchImages = b2.getSelection();
				daemon.setConfiguration(config);
			}
		});
	}
	
	private void addModulesSection() {
		Section section = toolkit.createSection(form.getBody(), Section.DESCRIPTION | Section.TITLE_BAR
				| Section.TWISTIE | Section.EXPANDED);
		section.setText("Web Spider Modules");
		Composite sectionClient = toolkit.createComposite(section);
		sectionClient.setLayout(new GridLayout());
		section.setClient(sectionClient);
		
		for(String moduleName: daemon.getAvailableModules()) {
			addModule(moduleName, sectionClient);
		}
	}
	
	private void addModule(final String moduleName, Composite parent) {
		final boolean enabled = daemon.isEnabled(moduleName);
		final Button b = toolkit.createButton(parent, moduleName, SWT.CHECK);
		b.setSelection(enabled);
		b.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				daemon.setEnabled(moduleName, b.getSelection());
			}
		});
	}

	private void addTargetsSection() {
		Section section = toolkit.createSection(form.getBody(), Section.DESCRIPTION | Section.TITLE_BAR
				| Section.TWISTIE | Section.EXPANDED);
		section.setText("Web Spider Targets");
		Composite sectionClient = toolkit.createComposite(section);
		sectionClient.setLayout(new GridLayout());
		section.setClient(sectionClient);
		
		for(IEntity e: Activator.getDefault().getCurrentSpace().getEntities()) {
			if (e instanceof WebSiteEntity) {
				WebSite site = new WebSite((HTTP)((WebSiteEntity) e).getHTTP().getAdapter(HTTP.class), ((WebSiteEntity) e).getHostName());
				addTarget(site, sectionClient);
			}
		}
	}
	
	private void addTarget(final WebSite site, Composite parent) {
		final boolean enabled = daemon.isEnabled(site);
		final Button b = toolkit.createButton(parent, site.toString(), SWT.CHECK);
		b.setSelection(enabled);
		b.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				daemon.setEnabled(site, b.getSelection());
			}
		});
	}

	protected void adjustBounds() {
		getShell().pack();
	}
}
