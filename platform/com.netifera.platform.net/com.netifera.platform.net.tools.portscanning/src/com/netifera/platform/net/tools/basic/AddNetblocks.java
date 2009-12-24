package com.netifera.platform.net.tools.basic;

import com.netifera.platform.api.tools.ITool;
import com.netifera.platform.api.tools.IToolContext;
import com.netifera.platform.api.tools.ToolException;
import com.netifera.platform.net.internal.tools.portscanning.Activator;
import com.netifera.platform.net.model.NetblockEntity;
import com.netifera.platform.tools.RequiredOptionMissingException;
import com.netifera.platform.util.addresses.inet.InternetNetblock;

public class AddNetblocks implements ITool {
	
	private IToolContext context;
	private InternetNetblock[] netblocks;

	public void run(IToolContext context) throws ToolException {
		this.context = context;

		setupToolOptions();

		context.setTitle(netblocks.length == 1 ? "Add netblock "+netblocks[0] : "Add multiple netblocks");

		for (InternetNetblock netblock: netblocks) {
			NetblockEntity entity = Activator.getInstance().getNetworkEntityFactory().createNetblock(context.getRealm(), context.getSpaceId(), netblock);
			entity.addTag("Target");
			entity.update();
			context.info("Netblock "+netblock+" added to the model");
		}
	}
	
	private void setupToolOptions() throws RequiredOptionMissingException {
		netblocks = (InternetNetblock[]) context.getConfiguration().get("netblocks");
		if(netblocks == null || netblocks.length == 0) {
			InternetNetblock netblock = (InternetNetblock) context.getConfiguration().get("netblock");
			if (netblock == null)
				throw new RequiredOptionMissingException("netblocks");
			netblocks = new InternetNetblock[] {netblock};
		}
	}
}
