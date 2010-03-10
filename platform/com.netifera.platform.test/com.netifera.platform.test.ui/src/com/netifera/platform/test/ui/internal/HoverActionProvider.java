package com.netifera.platform.test.ui.internal;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.IAction;

import com.netifera.platform.api.iterables.IndexedIterable;
import com.netifera.platform.api.model.IEntity;
import com.netifera.platform.test.tools.FakeScanner;
import com.netifera.platform.test.tools.TestConnect;
import com.netifera.platform.tools.options.GenericOption;
import com.netifera.platform.tools.options.IntegerOption;
import com.netifera.platform.tools.options.IterableOption;
import com.netifera.platform.tools.options.StringOption;
import com.netifera.platform.ui.actions.ToolAction;
import com.netifera.platform.ui.api.hover.IHoverActionProvider;
import com.netifera.platform.util.addresses.inet.InternetAddress;
import com.netifera.platform.util.addresses.inet.InternetNetblock;
import com.netifera.platform.util.addresses.inet.TCPSocketAddress;

public class HoverActionProvider implements IHoverActionProvider {

	@SuppressWarnings("unchecked")
	private IndexedIterable<InternetAddress> getInternetAddressIndexedIterable(Object o) {
		if (o instanceof InternetNetblock)
			return (InternetNetblock) o;
		if (o instanceof IEntity)
			return (IndexedIterable<InternetAddress>) ((IEntity)o).getIterableAdapter(InternetAddress.class);
		return null;
	}

	private ToolAction createFakeScanner(IndexedIterable<InternetAddress> addresses) {
		ToolAction scanner = new ToolAction("Fake Scan (test)", FakeScanner.class.getName());
		scanner.addFixedOption(new IterableOption(InternetAddress.class, "target", "Target", "Target addresses", addresses));
		scanner.addOption(new StringOption("ports", "Ports", "Ports to scan", "80"));
		scanner.addOption(new IntegerOption("delay", "Delay", "Milliseconds to wait between randomly creating service entities", 0));
		return scanner;
	}

	private ToolAction createTestConnect(TCPSocketAddress socketAddress) {
		ToolAction test = new ToolAction("Test Connect (test)", TestConnect.class.getName());
		test.addFixedOption(new GenericOption(TCPSocketAddress.class, "target", "Target", "Target socket address", socketAddress));
		test.addOption(new IntegerOption("delayBetweenConnections", "Delay", "Milliseconds to wait between connections", 10));
		test.addOption(new IntegerOption("numberOfConnections", "Number of connections", "Number of connections to make", 1000));
		test.addOption(new IntegerOption("connectTimeout", "Connect timeout", "Connect timeout in milliseconds", 5000));
		return test;
	}

	public List<IAction> getActions(Object o) {
		List<IAction> answer = new ArrayList<IAction>();

		IndexedIterable<InternetAddress> addresses = getInternetAddressIndexedIterable(o);
		if(addresses != null) {
			answer.add(createFakeScanner(addresses));
		}

		if (o instanceof IEntity) {
			TCPSocketAddress socketAddress = (TCPSocketAddress) ((IEntity)o).getAdapter(TCPSocketAddress.class);
			if (socketAddress != null)
				answer.add(createTestConnect(socketAddress));
		}
		return answer;
	}

	public List<IAction> getQuickActions(Object o) {
		return null;
	}
}
