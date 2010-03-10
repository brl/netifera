package com.netifera.platform.net.tools.basic;

import com.netifera.platform.api.tools.ITool;
import com.netifera.platform.api.tools.IToolContext;
import com.netifera.platform.api.tools.ToolException;
import com.netifera.platform.net.internal.tools.portscanning.Activator;
import com.netifera.platform.net.model.ServiceEntity;
import com.netifera.platform.tools.RequiredOptionMissingException;
import com.netifera.platform.util.addresses.inet.InternetSocketAddress;

public class AddService implements ITool {
	
	private IToolContext context;
	private InternetSocketAddress address;

	public void run(IToolContext context) throws ToolException {
		this.context = context;

		setupToolOptions();

		context.setTitle("Add "+address);
		ServiceEntity entity = Activator.getInstance().getNetworkEntityFactory().createService(context.getRealm(), context.getSpaceId(), address, null, null);
		entity.addTag("Target");
		entity.update();
		context.info("Service at "+address+" added to the model");
	}
	
	private void setupToolOptions() throws RequiredOptionMissingException {
		address = (InternetSocketAddress) context.getConfiguration().get("address");
		if(address == null) {
			throw new RequiredOptionMissingException("address");
		}
	}
}
