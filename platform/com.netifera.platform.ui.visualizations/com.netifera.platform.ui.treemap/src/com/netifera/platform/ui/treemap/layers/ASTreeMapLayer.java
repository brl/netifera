package com.netifera.platform.ui.treemap.layers;

import com.netifera.platform.net.routes.AS;
import com.netifera.platform.ui.internal.treemap.Activator;
import com.netifera.platform.util.addresses.inet.IPv4Netblock;


public class ASTreeMapLayer extends AbstractXKCDTreeMapLayer {

	protected String getCategory(IPv4Netblock netblock) {
		AS as = Activator.getInstance().getIP2ASService().getAS(netblock);
		return as == null ? null : as.getDescription();
	}

	public String getLayerName() {
		return "AS";
	}

	public boolean isDefaultEnabled() {
		return false;
	}

}