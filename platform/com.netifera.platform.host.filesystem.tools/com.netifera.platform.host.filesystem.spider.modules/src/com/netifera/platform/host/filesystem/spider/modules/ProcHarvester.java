package com.netifera.platform.host.filesystem.spider.modules;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.netifera.platform.host.filesystem.File;
import com.netifera.platform.host.filesystem.spider.IFileContent;
import com.netifera.platform.host.filesystem.spider.IFileSystemSpiderContext;
import com.netifera.platform.host.filesystem.spider.IFileSystemSpiderModule;
import com.netifera.platform.net.model.HostEntity;
import com.netifera.platform.net.model.InternetAddressEntity;

public class ProcHarvester implements IFileSystemSpiderModule {

	public String getName() {
		return "Harvest /proc";
	}

	public void handle(IFileSystemSpiderContext context, File file, IFileContent content) throws IOException {
		if (file.getAbsolutePath().equals("/proc/version")) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(content.getContentStream()));
			String line = reader.readLine();
			context.getLogger().info("Version: "+line);
			Activator.getInstance().getNetworkEntityFactory().setOperatingSystem(context.getRealm(), context.getSpaceId(), context.getHostAddress(), line);
		} else if (file.getAbsolutePath().equals("/proc/cpuinfo")) {
			InternetAddressEntity addressEntity = Activator.getInstance().getNetworkEntityFactory().createAddress(context.getRealm(), context.getSpaceId(), context.getHostAddress());
			HostEntity hostEntity = addressEntity.getHost();
			BufferedReader reader = new BufferedReader(new InputStreamReader(content.getContentStream()));
			String line = reader.readLine();
			while (line != null) {
				line = reader.readLine();
				if (line.startsWith("model name")) {
					String value = line.substring(line.indexOf(":")+1).trim();
					context.getLogger().info("CPU: "+value);
					hostEntity.setNamedAttribute("cpu", value);
				} else if (line.startsWith("bogomips")) {
					String value = line.substring(line.indexOf(":")+1).trim();
					context.getLogger().info("CPU bogomips: "+value);
					hostEntity.setNamedAttribute("bogomips", value);
					break;
				}
			}
			hostEntity.update();
		} else if (file.getAbsolutePath().equals("/proc/meminfo")) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(content.getContentStream()));
			String line = reader.readLine();
			if (line.startsWith("MemTotal")) {
				InternetAddressEntity addressEntity = Activator.getInstance().getNetworkEntityFactory().createAddress(context.getRealm(), context.getSpaceId(), context.getHostAddress());
				HostEntity hostEntity = addressEntity.getHost();
				String value = line.substring(line.indexOf(":")+1).trim();
				context.getLogger().info("Memory Total: "+value);
				hostEntity.setNamedAttribute("memoryTotal", value);
				line = reader.readLine();
				if (line.startsWith("MemFree")) {
					value = line.substring(line.indexOf(":")+1).trim();
					context.getLogger().info("Memory Free: "+value);
					hostEntity.setNamedAttribute("memoryFree", value);
				} else {
					context.getLogger().error("Malformed /proc/meminfo (expected MemFree): "+line);
				}
				hostEntity.update();
			} else {
				context.getLogger().error("Malformed /proc/meminfo (expected MemTotal): "+line);
			}
		} else if (file.getAbsolutePath().equals("/proc/uptime")) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(content.getContentStream()));
			String line = reader.readLine();
			context.getLogger().info("Uptime: "+line);
			InternetAddressEntity addressEntity = Activator.getInstance().getNetworkEntityFactory().createAddress(context.getRealm(), context.getSpaceId(), context.getHostAddress());
			HostEntity hostEntity = addressEntity.getHost();
			hostEntity.setNamedAttribute("uptime", line);
			hostEntity.update();
/*		} else if (file.getAbsolutePath().matches("^/proc/net/(udp|udp6|tcp|tcp6)$")) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(content.getContentStream()));
			String line = reader.readLine();
			line = reader.readLine();
			while (line != null) {
				line = reader.readLine();
			}
*/		}
	}

	public boolean isCompatible(String system) {
		return system.matches("(?i)linux");
	}

	public void start(IFileSystemSpiderContext context) {
		for (String each: new String[] {"version", "cpuinfo", "meminfo", "uptime"})
			context.getSpider().fetch("/proc/"+each);
//		for (String each: new String[] {"udp", "udp6", "tcp", "tcp6"})
//			context.getSpider().fetch("/proc/net/"+each);
	}

	public void stop(IFileSystemSpiderContext context) {
	}
}
