package com.netifera.platform.ui.probe.wizard;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.Wizard;

import com.netifera.platform.ui.probe.Activator;

public class NewProbeWizard extends Wizard {

	private FirstPage firstPage;
	private TCPListenChannelConfigPage tcpListenPage;
	
	@Override
	public void addPages() {
		setWindowTitle("New Probe");
		
		ImageDescriptor image = Activator.getInstance().getImageCache().getDescriptor("icons/new_probe_wiz.png");
		
		firstPage = new FirstPage();
		firstPage.setImageDescriptor(image);
		
		tcpListenPage = new TCPListenChannelConfigPage();
		tcpListenPage.setImageDescriptor(image);
		
		addPage(firstPage);
		addPage(tcpListenPage);
	}
	
	@Override
	public boolean performFinish() {
		final String name = firstPage.getName();
		final String config = tcpListenPage.getConfigString();
		Activator.getInstance().getProbeManager().createProbe(name, config);
		return true;
	}
}
