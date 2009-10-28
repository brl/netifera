package com.netifera.platform.host.filesystem.spider.modules;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.netifera.platform.host.filesystem.File;
import com.netifera.platform.host.filesystem.spider.IFileContent;
import com.netifera.platform.host.filesystem.spider.IFileSystemSpiderContext;
import com.netifera.platform.host.filesystem.spider.IFileSystemSpiderModule;
import com.netifera.platform.net.model.UserEntity;

public class ShadowHarvester implements IFileSystemSpiderModule {

	public String getName() {
		return "Harvest /etc/shadow";
	}

	public void handle(IFileSystemSpiderContext context, File file, IFileContent content) throws IOException {
		if (file.getAbsolutePath().equals("/etc/shadow")) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(content.getContentStream()));
			String line = reader.readLine();
			while (line != null) {
				String[] parts = line.split(":");
				String username = parts[0];
				String hash = parts[1];
				UserEntity userEntity = Activator.getInstance().getNetworkEntityFactory().createUser(context.getRealm(), context.getSpaceId(), context.getHostAddress(), username);
				if (hash.length() > 1) {
					userEntity.setLocked(false);
					userEntity.setHash("UNIX", hash);
					userEntity.update();
				} else {
					userEntity.setLocked(true);
					userEntity.update();
				}
				line = reader.readLine();
			}
		}
	}

	public boolean isCompatible(String system) {
		return !system.matches("(?i)windows|win32");
	}

	public void start(IFileSystemSpiderContext context) {
		context.getSpider().fetch("/etc/shadow");
	}

	public void stop(IFileSystemSpiderContext context) {
	}
}
