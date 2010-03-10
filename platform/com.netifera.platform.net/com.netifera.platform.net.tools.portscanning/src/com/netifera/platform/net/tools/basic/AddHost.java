package com.netifera.platform.net.tools.basic;

import com.netifera.platform.api.tools.ITool;
import com.netifera.platform.api.tools.IToolContext;
import com.netifera.platform.api.tools.ToolException;
import com.netifera.platform.net.internal.tools.portscanning.Activator;
import com.netifera.platform.net.model.HostEntity;
import com.netifera.platform.net.model.InternetAddressEntity;
import com.netifera.platform.tools.RequiredOptionMissingException;
import com.netifera.platform.util.addresses.inet.InternetAddress;

public class AddHost implements ITool {
	
	private IToolContext context;
	private InternetAddress address;

	public void run(IToolContext context) throws ToolException {
		this.context = context;

		setupToolOptions();

		context.setTitle("Add host "+address);
		InternetAddressEntity addressEntity = Activator.getInstance().getNetworkEntityFactory().createAddress(context.getRealm(), context.getSpaceId(), address);
		HostEntity entity = addressEntity.getHost();
		entity.addTag("Target");
		entity.update();
		context.info("Host "+address+" added to the model");
	}
	
	private void setupToolOptions() throws RequiredOptionMissingException {
		address = (InternetAddress) context.getConfiguration().get("address");
		if(address == null) {
			throw new RequiredOptionMissingException("address");
		}
	}
}
