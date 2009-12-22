package com.netifera.platform.ui.probe.wizard;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.Wizard;

import com.netifera.platform.api.model.IEntity;
import com.netifera.platform.ui.probe.Activator;
import com.netifera.platform.util.addresses.inet.InternetAddress;

public class NewProbeWizard extends Wizard {

	final private long spaceId;
	final private IEntity hostEntity;
	final private InternetAddress hostAddress;
	
	private FirstPage firstPage;
	private TCPListenChannelConfigPage tcpListenPage;

	public NewProbeWizard(long spaceId, IEntity hostEntity, InternetAddress hostAddress) {
		this.spaceId = spaceId;
		this.hostEntity = hostEntity;
		this.hostAddress = hostAddress;
	}
	
	@Override
	public void addPages() {
		setWindowTitle("New Probe");
		
		ImageDescriptor image = Activator.getInstance().getImageCache().getDescriptor("icons/new_probe_wiz.png");
		
		firstPage = new FirstPage();
		firstPage.setImageDescriptor(image);
		
		tcpListenPage = new TCPListenChannelConfigPage(hostAddress);
		tcpListenPage.setImageDescriptor(image);
		
		addPage(firstPage);
		addPage(tcpListenPage);
	}
	
	@Override
	public boolean performFinish() {
		final String name = firstPage.getName();
		final String config = tcpListenPage.getConfigString();
		Activator.getInstance().getProbeManager().createProbe(hostEntity, name, config, spaceId);
		return true;
	}
}
