package com.netifera.platform.ui.navigator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;

import com.netifera.platform.api.events.IEvent;
import com.netifera.platform.api.events.IEventHandler;
import com.netifera.platform.api.model.IEntity;
import com.netifera.platform.api.model.ISpace;
import com.netifera.platform.api.model.ISpaceStatusChangeEvent;
import com.netifera.platform.api.model.IWorkspace;
import com.netifera.platform.api.probe.IProbe;
import com.netifera.platform.model.ProbeEntity;
import com.netifera.platform.model.SpaceEntity;
import com.netifera.platform.ui.internal.navigator.Activator;
import com.netifera.platform.ui.updater.StructuredViewerUpdater;

public class NavigatorContentProvider implements ITreeContentProvider, IEventHandler {

	private IWorkspace workspace;
	private StructuredViewerUpdater updater;

	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof IProbe) {
			IProbe parentProbe = (IProbe)parentElement;
			Integer probeId = (int)parentProbe.getProbeId();
	
			List<IProbe> probes = new ArrayList<IProbe>();
			for (IProbe probe: Activator.getInstance().getProbeManager().getProbeList()) {
				if (getParentProbeId(probe) == probeId)
					probes.add(probe);
			}
			Collections.sort(probes, new Comparator<IProbe>() {
				public int compare(IProbe p1, IProbe p2) {
					if(p1.isLocalProbe()) {
						return -1;
					}
					if(p2.isLocalProbe()) {
						return 1;
					}
					if(p1.isConnected() && !p2.isConnected()) {
						return -1;
					}
					if(p2.isConnected() && !p1.isConnected()) {
						return 1;
					}
					return p1.getName().compareTo(p2.getName());
				}
			});

			List<ISpace> spaces = new ArrayList<ISpace>();
			for (ISpace space: workspace.getAllSpaces()) {
				if (space.getProbeId() == probeId) {
					IEntity rootEntity = space.getRootEntity();
					if (rootEntity instanceof SpaceEntity) {
						if (space.isIsolated()) {
							SpaceEntity spaceEntity = (SpaceEntity)rootEntity;
							if (spaceEntity.getRealmId() == parentProbe.getEntity().getId())
								spaces.add(space);
						}
					} else {
						spaces.add(space);
					}
				}
			}
			Collections.sort(spaces, new Comparator<ISpace>() {
				public int compare(ISpace o1, ISpace o2) {
					if(o1.getId() > o2.getId()) {
						return 1;
					} else if(o1.getId() < o2.getId()) {
						return -1;
					} else {
						return 0;
					}
				}
			});
			
			List<Object> elements = new ArrayList<Object>();
			elements.addAll(probes);
			elements.addAll(spaces);
			return elements.size() > 0 ? elements.toArray() : null;
		}

		if (parentElement instanceof ISpace) {
			ISpace parentSpace = (ISpace)parentElement;
			if (!parentSpace.isIsolated())
				return null;
			
			List<ISpace> spaces = new ArrayList<ISpace>();
			for (ISpace space: workspace.getAllSpaces()) {
				if (parentSpace.getId() == space.getId())
					continue;
				
				if (parentSpace.getRootEntity().getId() == space.getRootEntity().getId())
					spaces.add(space);
				else if (space.isIsolated() && space.getRootEntity().getRealmId() == parentSpace.getRootEntity().getId())
					spaces.add(space);
			}
			Collections.sort(spaces, new Comparator<ISpace>() {
				public int compare(ISpace o1, ISpace o2) {
					if(o1.getId() > o2.getId()) {
						return 1;
					} else if(o1.getId() < o2.getId()) {
						return -1;
					} else {
						return 0;
					}
				}
			});
			
			return spaces.size() > 0 ? spaces.toArray() : null;
		}

		return null;
	}

	public Object getParent(Object element) {
		if (element instanceof ISpace) {
			ISpace space = (ISpace)element;
			IEntity rootEntity = space.getRootEntity();
			if (rootEntity instanceof SpaceEntity) {
				return workspace.findSpaceById(((SpaceEntity)rootEntity).getSpaceId());
			} else {
				return Activator.getInstance().getProbeManager().getProbeById(space.getProbeId());
			}
		} else if (element instanceof IProbe) {
			return Activator.getInstance().getProbeManager().getProbeById(getParentProbeId((IProbe)element));
		}
		return null;
	}

	public boolean hasChildren(Object element) {
		return getChildren(element) != null;
	}

	//FIXME why not use the argument inputElement ???
	public Object[] getElements(Object inputElement) {
		return new IProbe[] {Activator.getInstance().getProbeManager().getLocalProbe()};
	}

	public void dispose() {
		updater = null;
		if(workspace != null) {
			workspace.removeSpaceStatusChangeListener(this);
		}
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if(newInput instanceof IWorkspace) {
			/* if workspace change remove handler from old and add to new */
			final IWorkspace workspace = (IWorkspace) newInput;
			if(this.workspace != workspace) {
				if(this.workspace != null) {
					this.workspace.removeSpaceStatusChangeListener(this);
				}
				workspace.addSpaceStatusChangeListener(this);
				this.workspace = workspace;
			} 
			
			updater = StructuredViewerUpdater.get((StructuredViewer) viewer);
		}		
	}
	
	public void handleEvent(final IEvent event) {
		if(event instanceof ISpaceStatusChangeEvent) {
			final ISpaceStatusChangeEvent spaceChange = (ISpaceStatusChangeEvent) event;
			if (spaceChange.isNew())
				updater.refresh();
			else
				updater.refresh(spaceChange.getSpace());
		}
	}
	
	private Integer getParentProbeId(IProbe probe) {
		if (probe.isLocalProbe())
			return null;
		IEntity parent = probe.getEntity();
		do {
			parent = workspace.findById(parent.getRealmId());
			if (parent instanceof ProbeEntity) {
				return ((ProbeEntity) parent).getProbeId();
			}
		} while (parent != null);
		return null;
	}
}
