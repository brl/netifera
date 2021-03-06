package com.netifera.platform.net.dns.tools;

import java.util.List;
import java.util.Locale;

import org.xbill.DNS.Lookup;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.TXTRecord;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

import com.netifera.platform.api.iterables.IndexedIterable;
import com.netifera.platform.api.tools.ITool;
import com.netifera.platform.api.tools.IToolContext;
import com.netifera.platform.api.tools.ToolException;
import com.netifera.platform.net.dns.internal.tools.Activator;
import com.netifera.platform.net.dns.service.nameresolver.INameResolver;
import com.netifera.platform.net.model.InternetAddressEntity;
import com.netifera.platform.util.addresses.inet.IPv4Address;
import com.netifera.platform.util.addresses.inet.InternetAddress;

public class NetOpGeoLocalizer implements ITool {
	private IToolContext context;
	private INameResolver resolver;
	private IndexedIterable<InternetAddress> addresses;
	
	public void run(IToolContext context) throws ToolException {
		this.context = context;

		setupToolOptions();
		context.setTitle("Geo-localize " + addresses);
		
		resolver = Activator.getInstance().getNameResolver();

		// FIXME
		if (addresses.size() > 0 && !(addresses.get(0) instanceof IPv4Address)) {
			context.warning("IPv6 not yet supported by NetOp");
			context.done();
			return;
		}
		context.setTotalWork(addresses.size());
		for (InternetAddress address: addresses) {
			localizate(address);
		}
		
		context.done();
	}

	@SuppressWarnings("unchecked")
	private void setupToolOptions() {
		addresses = (IndexedIterable<InternetAddress>) context.getConfiguration().get("target");
	}
	
	private void localizate(final InternetAddress address) {
		if (!(address instanceof IPv4Address)) {
			// FIXME
			context.warning("IPv6 not yet supported by NetOp");
			return;
		}
		
		context.setSubTitle("Localizing "+address);
		
		// XXX lame
		String[] octets = address.toString().split("\\.");
		InternetAddress revAddress = InternetAddress.fromString(octets[3]+"."+octets[2]+"."+octets[1]+"."+octets[0]); // IPv4 only

		Lookup lookup;
		String revName = revAddress.toString();
		try {
			lookup = new Lookup(revName + ".country.netop.org.", Type.TXT);
		} catch (TextParseException e) {
			context.warning("Malformed host name: " + revName);
			return;
		}
		lookup.setResolver(resolver.getExtendedResolver());
		lookup.setSearchPath((Name[])null);

		Record [] records = lookup.run();
		context.worked(1);
		if (records != null && records.length != 0) {
			for (Record record: records) {
				if (record instanceof TXTRecord) {
					List<String> list = txtStrings(record);
					if (!list.isEmpty()) {
						String countryCode = list.get(0);
						if (!countryCode.equals("ZZ")) {
							Locale locale = new Locale("en", list.get(0));
							String countryName = locale.getDisplayCountry(Locale.ENGLISH);
							context.info(address.toString() + " is in " + countryName);
							
							InternetAddressEntity entity = Activator.getInstance().getNetworkEntityFactory().createAddress(context.getRealm(), context.getSpaceId(), address);
							entity.setAttribute("country", countryCode);
							entity.update();
							entity.getHost().setAttribute("country", countryCode);
//							entity.getHost().addTag(countryName);
							entity.getHost().update(); //HACK or the tree builder never gets called to update
							
	//						Activator.getInstance().getNetworkEntityFactory().createAttributeFolder(context.getRealm(), context.getViewId(), countryName, "host", "country", countryCode);
							return;
						}
					}
				} else {
					context.warning("Unhandled record: " + record);
				}
			}
		}
		context.info("No TXT records found for " + address.toString());
	}
	
	@SuppressWarnings("unchecked")
	private List<String> txtStrings(Record record) {
		return ((TXTRecord)record).getStrings();
	}
}
