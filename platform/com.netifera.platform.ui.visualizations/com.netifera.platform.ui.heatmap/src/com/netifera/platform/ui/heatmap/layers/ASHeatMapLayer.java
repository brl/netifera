package com.netifera.platform.ui.heatmap.layers;

import com.netifera.platform.net.ui.routes.AS;
import com.netifera.platform.ui.internal.heatmap.Activator;
import com.netifera.platform.util.addresses.inet.IPv4Netblock;


public class ASHeatMapLayer extends AbstractXKCDHeatMapLayer {

	protected String getCategory(IPv4Netblock netblock) {
		AS as = Activator.getInstance().getIP2ASService().getAS(netblock);
		return as == null ? null : as.getDescription();
	}

	public String getName() {
		return "AS";
	}

	public boolean isDefaultEnabled() {
		return false;
	}

}