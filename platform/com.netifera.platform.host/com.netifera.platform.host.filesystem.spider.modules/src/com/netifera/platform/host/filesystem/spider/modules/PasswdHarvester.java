package com.netifera.platform.host.filesystem.spider.modules;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.netifera.platform.host.filesystem.File;
import com.netifera.platform.host.filesystem.spider.IFileContent;
import com.netifera.platform.host.filesystem.spider.IFileSystemSpiderContext;
import com.netifera.platform.host.filesystem.spider.IFileSystemSpiderModule;
import com.netifera.platform.net.model.UserEntity;

public class PasswdHarvester implements IFileSystemSpiderModule {

	public String getName() {
		return "Harvest /etc/passwd and /etc/shadow";
	}

	public void handle(IFileSystemSpiderContext context, File file, IFileContent content) {
		try {
			if (file.getAbsolutePath().equals("/etc/passwd")) {
				BufferedReader reader = new BufferedReader(new InputStreamReader(content.getContentStream()));
				String line = reader.readLine();
				while (line != null) {
					String[] parts = line.split(":");
					String username = parts[0];
					String home = parts[5];
					UserEntity userEntity = Activator.getInstance().getNetworkEntityFactory().createUser(context.getRealm(), context.getSpaceId(), context.getHostAddress(), username);
					userEntity.setHome(home);
					userEntity.update();
					line = reader.readLine();
				}
			} else if (file.getAbsolutePath().equals("/etc/shadow")) {
				BufferedReader reader = new BufferedReader(new InputStreamReader(content.getContentStream()));
				String line = reader.readLine();
				while (line != null) {
					String[] parts = line.split(":");
					String username = parts[0];
					String hash = parts[1];
					UserEntity userEntity = Activator.getInstance().getNetworkEntityFactory().createUser(context.getRealm(), context.getSpaceId(), context.getHostAddress(), username);
					if (hash.length() > 1) {
						userEntity.setHash("UNIX", hash);
						userEntity.update();
					}
					line = reader.readLine();
				}
			}
		} catch (IOException e) {
			context.getLogger().error("Error reading "+file, e);
		}
	}

	public boolean isCompatible(String system) {
		return !system.matches("(?i)windows|win32");
	}

	public void start(IFileSystemSpiderContext context) {
		for (String each: new String[] {"shadow", "passwd"})
			context.getSpider().fetch("/etc/"+each);
	}

	public void stop(IFileSystemSpiderContext context) {
	}
}
