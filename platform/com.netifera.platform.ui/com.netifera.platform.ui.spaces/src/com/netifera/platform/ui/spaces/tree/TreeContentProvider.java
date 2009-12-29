package com.netifera.platform.ui.spaces.tree;

import java.util.ArrayList;
import java.util.Collections;
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
import com.netifera.platform.model.TreeStructureContext;
import com.netifera.platform.ui.internal.spaces.Activator;
import com.netifera.platform.ui.updater.StructuredViewerUpdater;

public class TreeContentProvider implements ITreeContentProvider {
	private ISpace space;
	private TreeBuilder treeBuilder;
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
	
	public TreeBuilder getTreeBuilder() {
		return treeBuilder;
	}
	
	private List<IShadowEntity> getChildEntities(IShadowEntity entity) {
		TreeStructureContext tsc = nodeToTSC(entity);
		if(tsc.hasChildren()) {
			return tsc.getChildren();
		} else {
			return Collections.emptyList();
		}
	}
	
	public Object getParent(Object node) {
		return nodeToTSC(node).getParent();
	}

	public boolean hasChildren(Object node) {
		return nodeToTSC(node).hasChildren();
	}

	public Object[] getElements(Object input) {
		if(input != space) {
			throw new IllegalArgumentException();
		}
		return getChildEntities(treeBuilder.getRoot()).toArray();
	}
	
	public void dispose() {
		if (space != null)
			space.removeChangeListener(spaceListener);
		if (loadJob != null)
			loadJob.cancel();
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if(!(viewer instanceof StructuredViewer) || !(newInput instanceof ISpace)) {
			return;
		}
		
		if (space != null)
			space.removeChangeListener(spaceListener);
		if (loadJob != null)
			loadJob.cancel();
		
		this.space = (ISpace) newInput;
//		this.viewer = treeViewer;
		this.updater = StructuredViewerUpdater.get((TreeViewer)viewer);
		
		List<ISemanticLayer> layerProviders = new ArrayList<ISemanticLayer>();
		for (ISemanticLayer layerProvider: Activator.getInstance().getModel().getSemanticLayers())
			if (layerProvider.isDefaultEnabled())
				layerProviders.add(layerProvider);
		
		this.treeBuilder = new TreeBuilder(layerProviders);
		this.treeBuilder.setListener(createUpdateListener());

		space.addChangeListener(spaceListener);

		loadSpace();
	}
	
	private void handleSpaceChange(ISpaceContentChangeEvent event) {
		if(event.isEntityAddEvent()) {
			treeBuilder.addEntity(event.getEntity());
		} else if(event.isEntityUpdateEvent()) {
			if(treeBuilderHasValidRoot())
				treeBuilder.updateEntity(event.getEntity());
		} else if(event.isEntityRemoveEvent()) {
			treeBuilder.removeEntity(event.getEntity());
		}
	}

	private ITreeBuilderListener createUpdateListener() {
		return new ITreeBuilderListener() {

			public void entityAdded(IShadowEntity entity, IShadowEntity parent) {
				if (parent == treeBuilder.getRoot())
					updater.refresh();
				updater.refresh(parent);
			}

			public void entityChanged(IShadowEntity entity) {
				updater.update(entity,null);
			}

			public void entityRemoved(IShadowEntity entity, IShadowEntity parent) {
				if (parent == treeBuilder.getRoot())
					updater.refresh();
				updater.refresh(parent);
			}
		};
	}

	private boolean treeBuilderHasValidRoot() {
		return (treeBuilder.getRoot() != null && (treeBuilder.getRoot().getStructureContext() instanceof TreeStructureContext));
	}

	/*
	 * Convert a tree node to the corresponding TreeStructureContext
	 */
	private TreeStructureContext nodeToTSC(Object node) {
		if(node instanceof IShadowEntity) {
			IStructureContext sc = ((IShadowEntity)node).getStructureContext();
			
			if(sc instanceof TreeStructureContext) {
				return (TreeStructureContext) sc;
			}
						
		}
		
		throw new IllegalStateException("Could not convert node to TreeStructureContext");		
	}
	
	public List<ISemanticLayer> getLayers() {
		return treeBuilder.getLayers();
	}
	
	public void addLayer(ISemanticLayer layerProvider) {
		treeBuilder.addLayer(layerProvider);
		loadSpace();
	}
	
	public void removeLayer(ISemanticLayer layerProvider) {
		treeBuilder.removeLayer(layerProvider);
		loadSpace();
	}
	
	private void loadSpace() {
		if (loadJob != null) {
			loadJob.cancel();
			Thread.yield();
		}
		treeBuilder.setRoot(space.getRootEntity());
		loadJob = new Job("Loading space '"+space.getName()+"'") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask("Loading entities", space.entityCount());
				for(IEntity entity: space) {
					treeBuilder.addEntity(entity);
					monitor.worked(1);
					if (monitor.isCanceled()) {
						return Status.CANCEL_STATUS;
					}
				}
				
				updater.refresh();
				return Status.OK_STATUS;
			}
		};
		loadJob.setPriority(Job.SHORT);
		loadJob.schedule();
	}
}
