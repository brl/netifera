package com.netifera.platform.host.internal.filesystem.ui;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.action.IAction;

import com.netifera.platform.api.model.IShadowEntity;
import com.netifera.platform.host.filesystem.FileSystemServiceLocator;
import com.netifera.platform.host.filesystem.ui.OpenFileSystemViewAction;
import com.netifera.platform.ui.actions.SpaceAction;
import com.netifera.platform.ui.api.hover.IHoverActionProvider;

public class HoverActionProvider implements IHoverActionProvider {

	public List<IAction> getActions(Object o) {
		return Collections.emptyList();
	}

	public List<IAction> getQuickActions(Object o) {
		if (!(o instanceof IShadowEntity)) return Collections.emptyList();
		IShadowEntity shadow = (IShadowEntity) o;

		List<IAction> answer = new ArrayList<IAction>();
		
		final FileSystemServiceLocator fileSystemLocator = (FileSystemServiceLocator) shadow.getAdapter(FileSystemServiceLocator.class);
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
}
