package com.netifera.platform.host.internal.filesystem.ui;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.action.IAction;

import com.netifera.platform.api.log.ILogManager;
import com.netifera.platform.api.log.ILogger;
import com.netifera.platform.api.model.IShadowEntity;
import com.netifera.platform.api.probe.IProbeManagerService;
import com.netifera.platform.host.filesystem.FileSystemLocator;
import com.netifera.platform.host.filesystem.ui.OpenFileSystemViewAction;
import com.netifera.platform.ui.actions.SpaceAction;
import com.netifera.platform.ui.api.actions.IEntityActionProvider;

public class EntityActionProvider implements IEntityActionProvider {

	private ILogger logger;
	private IProbeManagerService probeManager;
	
	public List<IAction> getActions(IShadowEntity shadow) {
		return Collections.emptyList();
	}

	public List<IAction> getQuickActions(IShadowEntity shadow) {
		List<IAction> answer = new ArrayList<IAction>();
		
		final FileSystemLocator fileSystemLocator = (FileSystemLocator) shadow.getAdapter(FileSystemLocator.class);
		if (fileSystemLocator != null) {
			SpaceAction action = new OpenFileSystemViewAction("Browse File System") {
				@Override
				public URI getFileSystemURL() {
					return fileSystemLocator.getURL();
				}
			};
			answer.add(action);
		}
		
		return answer;
	}

	protected void setLogManager(ILogManager logManager) {
		logger = logManager.getLogger("File System Actions");
	}
	
	protected void unsetLogManager(ILogManager logManager) {
		
	}
	protected void setProbeManager(IProbeManagerService probeManager) {
		this.probeManager = probeManager;
	}
	
	protected void unsetProbeManager(IProbeManagerService probeManager) {
		
	}
}
