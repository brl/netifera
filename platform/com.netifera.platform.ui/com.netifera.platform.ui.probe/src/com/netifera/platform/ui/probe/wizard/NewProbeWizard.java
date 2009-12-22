package com.netifera.platform.ui.probe.wizard;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.Wizard;

import com.netifera.platform.net.model.HostEntity;
import com.netifera.platform.ui.probe.Activator;
import com.netifera.platform.util.addresses.inet.InternetAddress;

public class NewProbeWizard extends Wizard {

	final private long spaceId;
	final private HostEntity hostEntity;
	
	private FirstPage firstPage;
	private TCPListenChannelConfigPage tcpListenPage;

	public NewProbeWizard(long spaceId, HostEntity hostEntity) {
		this.spaceId = spaceId;
		this.hostEntity = hostEntity;
	}
	
	@Override
	public void addPages() {
		setWindowTitle("New Probe");
		
		ImageDescriptor image = Activator.getInstance().getImageCache().getDescriptor("icons/new_probe_wiz.png");
		
		firstPage = new FirstPage();
		firstPage.setImageDescriptor(image);
		
		InternetAddress address = hostEntity == null ? null : (InternetAddress)hostEntity.getDefaultAddress().toNetworkAddress();
		tcpListenPage = new TCPListenChannelConfigPage(address);
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
