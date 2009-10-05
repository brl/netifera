package com.netifera.platform.net.cifs.internal.ui;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.action.IAction;

import com.netifera.platform.api.model.IShadowEntity;
import com.netifera.platform.api.tools.IToolConfiguration;
import com.netifera.platform.host.filesystem.IFileSystem;
import com.netifera.platform.host.filesystem.ui.OpenFileSystemViewAction;
import com.netifera.platform.net.cifs.filesystem.SMBFileSystem;
import com.netifera.platform.net.model.ServiceEntity;
import com.netifera.platform.tools.options.GenericOption;
import com.netifera.platform.tools.options.StringOption;
import com.netifera.platform.ui.actions.SpaceAction;
import com.netifera.platform.ui.api.actions.IEntityActionProvider;
import com.netifera.platform.util.locators.TCPSocketLocator;

public class EntityActionProvider implements IEntityActionProvider {

	
	public List<IAction> getActions(IShadowEntity entity) {
		return Collections.emptyList();
	}

	public List<IAction> getQuickActions(IShadowEntity entity) {
		List<IAction> answer = new ArrayList<IAction>();

		if (entity instanceof ServiceEntity) {
			ServiceEntity serviceEntity = (ServiceEntity) entity;
			TCPSocketLocator locator = (TCPSocketLocator) serviceEntity.getAdapter(TCPSocketLocator.class);
			if (locator != null && serviceEntity.getServiceType().equals("NetBIOS-SSN")) {
				SpaceAction action = new OpenFileSystemViewAction("Browse File System") {
					@Override
					public IFileSystem createFileSystem() {
						IToolConfiguration config = getConfiguration();
						TCPSocketLocator target = (TCPSocketLocator) config.get("target");
						String domain = (String) config.get("domain");
						String username = (String) config.get("username");
						String password = (String) config.get("password");
						String url = "smb://";
						if (username.length() > 0) {
							if (domain.length() > 0)
								url += domain+";";
							url += username+":"+password+"@";
						}
						url += target.getAddress();
						if (target.getPort() != 139)
							url += ":"+target.getPort();
						url += "/";
						try {
							return new SMBFileSystem(url);
						} catch (MalformedURLException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							throw new RuntimeException(e);
						}
					}
				};
				action.addFixedOption(new GenericOption(TCPSocketLocator.class, "target", "Target", "Target server to connect to", locator));
				action.addOption(new StringOption("username", "Username", "", "Administrator"));
				action.addOption(new StringOption("password", "Password", "", "", true));
				action.addOption(new StringOption("domain", "Domain", "", "", true));
				answer.add(action);
			}
		}

		return answer;
	}
}
