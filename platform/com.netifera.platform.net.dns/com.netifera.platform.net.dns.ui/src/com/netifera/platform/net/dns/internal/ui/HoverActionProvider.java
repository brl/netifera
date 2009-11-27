package com.netifera.platform.net.dns.internal.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.IAction;

import com.netifera.platform.api.iterables.IndexedIterable;
import com.netifera.platform.api.model.IEntity;
import com.netifera.platform.api.model.IShadowEntity;
import com.netifera.platform.net.dns.model.DomainEntity;
import com.netifera.platform.net.dns.model.NSRecordEntity;
import com.netifera.platform.net.dns.service.DNS;
import com.netifera.platform.net.dns.tools.DNSReverseLookup;
import com.netifera.platform.net.dns.tools.DNSZoneTransfer;
import com.netifera.platform.net.dns.tools.HostNamesBruteforcer;
import com.netifera.platform.net.dns.tools.MXLookup;
import com.netifera.platform.net.dns.tools.NSLookup;
import com.netifera.platform.net.model.ServiceEntity;
import com.netifera.platform.tools.options.BooleanOption;
import com.netifera.platform.tools.options.GenericOption;
import com.netifera.platform.tools.options.IterableOption;
import com.netifera.platform.tools.options.StringOption;
import com.netifera.platform.ui.actions.ToolAction;
import com.netifera.platform.ui.api.hover.IHoverActionProvider;
import com.netifera.platform.util.addresses.inet.InternetAddress;
import com.netifera.platform.util.addresses.inet.InternetNetblock;

public class HoverActionProvider implements IHoverActionProvider {

	public List<IAction> getActions(Object o) {
		List<IAction> answer = new ArrayList<IAction>();

		IndexedIterable<InternetAddress> addresses = getInternetAddressIndexedIterable(o);
		if (addresses != null) {
			ToolAction reverseLookup = new ToolAction("Reverse DNS Lookup", DNSReverseLookup.class.getName());
			reverseLookup.addFixedOption(new IterableOption(InternetAddress.class, "target", "Target", "Addresses to reverse-lookup", addresses));
			reverseLookup.addOption(new GenericOption(DNS.class, "dns", "Name Server", "Target Name Server", null));
			answer.add(reverseLookup);
		}

		if (!(o instanceof IShadowEntity)) return answer;
		IShadowEntity entity = (IShadowEntity) o;

		if (entity instanceof DomainEntity) {
			String domain = ((DomainEntity)entity).getFQDM();
			
			ToolAction nsLookup = new ToolAction("Lookup NS records for "+domain, NSLookup.class.getName());
			nsLookup.addFixedOption(new StringOption("domain", "Domain", "Target domain", domain));
			nsLookup.addOption(new GenericOption(DNS.class, "dns", "Name Server", "Target Name Server", null));
			answer.add(nsLookup);

			ToolAction mxLookup = new ToolAction("Lookup MX records for "+domain, MXLookup.class.getName());
			mxLookup.addFixedOption(new StringOption("domain", "Domain", "Target domain", domain));
			mxLookup.addOption(new GenericOption(DNS.class, "dns", "Name Server", "Target Name Server", null));
			answer.add(mxLookup);

			ToolAction hostNamesBruteforcer = new ToolAction("Lookup Common Host Names *."+domain, HostNamesBruteforcer.class.getName());
			hostNamesBruteforcer.addFixedOption(new StringOption("domain", "Domain", "Target domain", domain));
			hostNamesBruteforcer.addOption(new GenericOption(DNS.class, "dns", "Name Server", "Target Name Server", null));
			hostNamesBruteforcer.addOption(new BooleanOption("tryTLDs", "Try alternative TLDs", "Try alternative TLDs like "+domain.substring(0, domain.lastIndexOf("."))+".*", false));
			answer.add(hostNamesBruteforcer);
		}

		DNS dns = (DNS) entity.getAdapter(DNS.class);
		if (dns != null) {
			ToolAction zoneTransfer = new ToolAction("Zone Transfer", DNSZoneTransfer.class.getName());
			zoneTransfer.addOption(new StringOption("domain", "Domain", "Target domain", ""));
			zoneTransfer.addFixedOption(new GenericOption(DNS.class,"dns", "Name Server", "Target Name Server", dns));
			answer.add(zoneTransfer);
		}

		if (entity instanceof NSRecordEntity) {
			ServiceEntity service = ((NSRecordEntity)entity).getService();
			if (service != null) {
				dns = (DNS) service.getAdapter(DNS.class);
				ToolAction zoneTransfer = new ToolAction("Zone Transfer", DNSZoneTransfer.class.getName());
				zoneTransfer.addOption(new StringOption("domain", "Domain", "Target domain", ((NSRecordEntity)entity).getDomain().getFQDM()));
				zoneTransfer.addFixedOption(new GenericOption(DNS.class,"dns", "Name Server", "Target Name Server", dns));
				answer.add(zoneTransfer);
			}
		}
		
		return answer;
	}

	public List<IAction> getQuickActions(Object o) {
		// TODO Auto-generated method stub
		return null;
	}

	@SuppressWarnings("unchecked")
	private IndexedIterable<InternetAddress> getInternetAddressIndexedIterable(Object o) {
		if (o instanceof InternetNetblock)
			return (InternetNetblock) o;
		if (o instanceof IEntity)
			return (IndexedIterable<InternetAddress>) ((IEntity)o).getIterableAdapter(InternetAddress.class);
		return null;
	}

/*	@SuppressWarnings("unchecked")
	private IndexedIterable<InternetAddress> getIPv4AddressIndexedIterable(IEntity entity) {
		return (IndexedIterable<InternetAddress>) entity.getIterableAdapter(IPv4Address.class);
	}
*/
}
