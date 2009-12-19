package com.netifera.platform.demo.ui.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.action.IAction;

import com.netifera.platform.api.iterables.ListIndexedIterable;
import com.netifera.platform.api.model.IShadowEntity;
import com.netifera.platform.demo.ExploitTestService;
import com.netifera.platform.net.model.ServiceEntity;
import com.netifera.platform.tools.options.IterableOption;
import com.netifera.platform.tools.options.StringOption;
import com.netifera.platform.ui.actions.ToolAction;
import com.netifera.platform.ui.api.hover.IHoverActionProvider;
import com.netifera.platform.util.addresses.inet.InternetAddress;
import com.netifera.platform.util.addresses.inet.TCPSocketAddress;

public class HoverActionProvider implements IHoverActionProvider {
	
	public List<IAction> getActions(Object o) {
		if (!(o instanceof IShadowEntity)) return Collections.emptyList();
		IShadowEntity entity = (IShadowEntity) o;
		List<IAction> answer = new ArrayList<IAction>();
		if (entity instanceof ServiceEntity) {
			ServiceEntity serviceEntity = (ServiceEntity) entity;
			if ("TEST".equals(serviceEntity.getServiceType())) {
				TCPSocketAddress socketAddress = (TCPSocketAddress) entity.getAdapter(TCPSocketAddress.class);
				if (socketAddress != null) {
					ListIndexedIterable<InternetAddress> addresses = new ListIndexedIterable<InternetAddress>(socketAddress.getNetworkAddress());
					assert addresses.get(0).isUniCast();
					ToolAction exploit = new ToolAction("Exploit Test Service At "+socketAddress, ExploitTestService.class.getName());
					exploit.addFixedOption(new IterableOption(InternetAddress.class, "target", "Target", "Target addresses", addresses));
					exploit.addFixedOption(new StringOption("port", "Port", "Ports to exploit", ((Integer)socketAddress.getPort()).toString()));
					answer.add(exploit);
				}
			}
		}
		return answer;
	}

	public List<IAction> getQuickActions(Object o) {
		return Collections.emptyList();
	}
}
