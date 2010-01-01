package com.netifera.platform.ui.spaces.tree;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;

import com.netifera.platform.api.events.IEvent;
import com.netifera.platform.api.events.IEventHandler;
import com.netifera.platform.api.model.IEntity;
import com.netifera.platform.api.model.IShadowEntity;
import com.netifera.platform.api.model.ISpace;
import com.netifera.platform.api.model.IStructureContext;
import com.netifera.platform.api.model.events.ISpaceContentChangeEvent;
import com.netifera.platform.api.model.layers.ISemanticLayer;
import com.netifera.platform.ui.internal.spaces.Activator;
import com.netifera.platform.ui.updater.StructuredViewerUpdater;

public class TreeContentProvider implements ITreeContentProvider {
	private ISpace space;
	private Tree tree;
	private StructuredViewerUpdater updater;
	private Job loadJob;
	private final IEventHandler spaceListener = new IEventHandler() {
		public void handleEvent(final IEvent event) {
			if(event instanceof ISpaceContentChangeEvent) {
				handleSpaceChange((ISpaceContentChangeEvent)event);
			}
		}
	};

	public Object[] getChildren(Object node) {
		if(!(node instanceof IShadowEntity)) {
			throw new IllegalArgumentException();
		}
		final List<IShadowEntity> children = getChildEntities((IShadowEntity) node);
		
		return children.toArray();
	}
	
	public Tree getTree() {
		return tree;
	}
	
	private List<IShadowEntity> getChildEntities(IShadowEntity entity) {
		return getStructureContext(entity).getChildren();
	}
	
	public Object getParent(Object node) {
		return getStructureContext(node).getParent();
	}

	public boolean hasChildren(Object node) {
		return getStructureContext(node).hasChildren();
	}

	public Object[] getElements(Object input) {
		if(input != space) {
			throw new IllegalArgumentException();
		}
		return getChildEntities(tree.getRoot()).toArray();
	}
	
	public void dispose() {
		if (space != null)
			space.removeChangeListener(spaceListener);
		if (loadJob != null)
			loadJob.cancel();
		if (tree != null)
			tree.dispose();
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if(!(viewer instanceof StructuredViewer) || !(newInput instanceof ISpace)) {
			return;
		}

		// Remove listener, cancel pending load job, dispose the shadow entities kept by the TreeBuilder
		dispose();
		
		this.space = (ISpace) newInput;
//		this.viewer = treeViewer;
		this.updater = StructuredViewerUpdater.get((TreeViewer)viewer);
		
		List<ISemanticLayer> layerProviders = new ArrayList<ISemanticLayer>();
		for (ISemanticLayer layerProvider: Activator.getInstance().getModel().getSemanticLayers())
			if (layerProvider.isDefaultEnabled())
				layerProviders.add(layerProvider);
		
		this.tree = new Tree(layerProviders);
		this.tree.setChangeListener(createTreeChangeListener());

		space.addChangeListener(spaceListener);

		loadSpace();
	}
	
	private void handleSpaceChange(ISpaceContentChangeEvent event) {
		if(event.isEntityAddEvent()) {
			tree.addEntity(event.getEntity());
		} else if(event.isEntityUpdateEvent()) {
			if(treeHasValidRoot())
				tree.updateEntity(event.getEntity());
		} else if(event.isEntityRemoveEvent()) {
			tree.removeEntity(event.getEntity());
		}
	}

	private ITreeChangeListener createTreeChangeListener() {
		return new ITreeChangeListener() {
			public void entityAdded(IShadowEntity entity, IShadowEntity parent) {
				if (parent == tree.getRoot()) {
					updater.refresh();
				} else {
					updater.remove(getChildEntities(parent));
					updater.refresh(parent);
				}
			}

			public void entityChanged(IShadowEntity entity) {
				updater.update(entity,null);
			}

			public void entityRemoved(IShadowEntity entity, IShadowEntity parent) {
				if (parent == tree.getRoot()) {
					updater.refresh();
				} else {
					updater.remove(getChildEntities(parent));
					updater.refresh(parent);
				}
			}
		};
	}
	
	private ITreeChangeListener createNullTreeChangeListener() {
		return new ITreeChangeListener() {
			public void entityAdded(IShadowEntity entity, IShadowEntity parent) {}
			public void entityChanged(IShadowEntity entity) {}
			public void entityRemoved(IShadowEntity entity, IShadowEntity parent) {}
		};
	}

	private boolean treeHasValidRoot() {
		return (tree.getRoot() != null && (tree.getRoot().getStructureContext() instanceof TreeStructureContext));
	}

	/*
	 * Convert a tree node to the corresponding TreeStructureContext
	 */
	private TreeStructureContext getStructureContext(Object node) {
		if(node instanceof IShadowEntity) {
			IStructureContext sc = ((IShadowEntity)node).getStructureContext();
			if(sc instanceof TreeStructureContext)
				return (TreeStructureContext) sc;
		}
		
		throw new IllegalStateException("Could not convert node to TreeStructureContext");		
	}
	
	public List<ISemanticLayer> getLayers() {
		return tree.getLayers();
	}
	
	public void addLayer(ISemanticLayer layerProvider) {
		tree.addLayer(layerProvider);
		loadSpace();
	}
	
	public void removeLayer(ISemanticLayer layerProvider) {
		tree.removeLayer(layerProvider);
		loadSpace();
	}
	
	private void loadSpace() {
		if (loadJob != null) {
			loadJob.cancel();
			Thread.yield();
		}
		tree.setChangeListener(createNullTreeChangeListener());
		tree.setRoot(space.getRootEntity());
		loadJob = new Job("Loading space '"+space.getName()+"'") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask("Loading entities", space.size());
				for(IEntity entity: space) {
					tree.addEntity(entity);
					monitor.worked(1);
					if (monitor.isCanceled()) {
						return Status.CANCEL_STATUS;
					}
				}
				
				tree.setChangeListener(createTreeChangeListener());
				updater.refresh();
				return Status.OK_STATUS;
			}
		};
		loadJob.setPriority(Job.SHORT);
		loadJob.schedule();
	}
}
