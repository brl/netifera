package com.netifera.platform.ui.spaces.actions;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;

import com.netifera.platform.api.model.IEntity;
import com.netifera.platform.api.model.ISpace;
import com.netifera.platform.api.model.IWorkspace;
import com.netifera.platform.api.probe.IProbe;
import com.netifera.platform.api.probe.IProbeManagerService;
import com.netifera.platform.model.SpaceEntity;
import com.netifera.platform.ui.internal.spaces.Activator;
import com.netifera.platform.ui.spaces.SpaceEditorInput;
import com.netifera.platform.ui.spaces.editor.SpaceEditor;

public class SpaceCreator {
	private final IWorkbenchWindow window;
	
	public SpaceCreator(IWorkbenchWindow window) {
		this.window = window;
	}

	public void openNewSpace(boolean isolated) {
		openNewSpace(null, isolated);
	}

	public void openNewSpace(String name, boolean isolated) {
		IProbeManagerService probeManager = Activator.getInstance().getProbeManager();

		IEditorPart editor = window.getActivePage().getActiveEditor();
		if(editor != null) {
			IEditorInput input = editor.getEditorInput();
			if(input instanceof SpaceEditorInput)  {
				ISpace space = ((SpaceEditorInput)input).getSpace();
				openNewSpace(name, space, isolated);
				return;
			}
		}
		
		IProbe probe = probeManager.getLocalProbe();
		openNewSpace(name, probe, isolated);
	}

	public void openNewSpace(String name, IProbe probe, boolean isolated) {
		if (probe.isLocalProbe()) // force isolated spaces on the local probe
			isolated = true;
		IEntity rootEntity = isolated ? createSpaceEntity(probe, probe.getEntity()) : probe.getEntity();
		openNewSpace(name, probe, rootEntity);
	}

	public void openNewSpace(String name, ISpace parentOrSibling, boolean isolated) {
		IProbe probe = Activator.getInstance().getProbeManager().getProbeById(parentOrSibling.getProbeId());
		IEntity rootEntity = isolated ? createSpaceEntity(probe, parentOrSibling.getRootEntity()) : parentOrSibling.getRootEntity();
		openNewSpace(name, probe, rootEntity);
	}

	private void openNewSpace(String name, IProbe probe, IEntity rootEntity) {
		ISpace space = createSpace(rootEntity, probe);
		if (name != null) space.setName(name);
		openEditor(space);
	}

	private SpaceEntity createSpaceEntity(IProbe probe, IEntity realmEntity) {
		IWorkspace workspace = Activator.getInstance().getModel().getCurrentWorkspace();
		if (!probe.isLocalProbe())
			throw new IllegalArgumentException("Can't create isolated spaces on remote probes");
		SpaceEntity spaceEntity = new SpaceEntity(workspace, realmEntity);
		spaceEntity.save();
		return spaceEntity;
	}
	
	private ISpace createSpace(IEntity rootEntity, IProbe probe) {
		IWorkspace workspace = Activator.getInstance().getModel().getCurrentWorkspace();
		ISpace space = workspace.createSpace(rootEntity, probe);
		space.open();
		return space;
	}
	
	private void openEditor(ISpace space) {
		IEditorInput input = new SpaceEditorInput(space);
		try {
			window.getActivePage().openEditor(input, SpaceEditor.ID);
		} catch (PartInitException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
