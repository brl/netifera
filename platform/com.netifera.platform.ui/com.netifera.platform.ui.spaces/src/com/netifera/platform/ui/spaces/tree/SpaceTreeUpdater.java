package com.netifera.platform.ui.spaces.tree;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.StructuredViewer;

import com.netifera.platform.api.events.IEvent;
import com.netifera.platform.api.events.IEventHandler;
import com.netifera.platform.api.model.IEntity;
import com.netifera.platform.api.model.IShadowEntity;
import com.netifera.platform.api.model.ISpace;
import com.netifera.platform.api.model.events.ISpaceContentChangeEvent;
import com.netifera.platform.api.model.layers.ISemanticLayer;
import com.netifera.platform.model.TreeStructureContext;
import com.netifera.platform.ui.internal.spaces.Activator;
import com.netifera.platform.ui.updater.StructuredViewerUpdater;

public class SpaceTreeUpdater {
	private final ISpace space;
	private final TreeBuilder treeBuilder;
//	private final StructuredViewer viewer;
	private final StructuredViewerUpdater updater;
	private final IEventHandler spaceListener;
	private Thread populationThread;

	SpaceTreeUpdater(final ISpace space, final StructuredViewer treeViewer) {
		if(space == null || treeViewer == null) {
			throw new IllegalArgumentException("space=" + space + ", viewer=" + treeViewer);
		}
		this.space = space;
//		this.viewer = treeViewer;
		this.updater = StructuredViewerUpdater.get(treeViewer);
		List<ISemanticLayer> layerProviders = new ArrayList<ISemanticLayer>();
		for (ISemanticLayer layerProvider: Activator.getInstance().getModel().getSemanticLayers())
			if (layerProvider.isDefaultEnabled())
				layerProviders.add(layerProvider);
		this.treeBuilder = new TreeBuilder(layerProviders);
		this.treeBuilder.setRoot(space.getRootEntity());
		this.treeBuilder.setListener(createUpdateListener());

		this.spaceListener = createSpaceListener();
		
		treeViewer.getControl().setEnabled(false);
		treeViewer.getControl().setToolTipText("Loading...");
		populationThread = new Thread(new Runnable() {
			public void run() {
				space.addChangeListenerAndPopulate(spaceListener);
				updater.asyncExec(new Runnable() {
					public void run() {
						treeViewer.getControl().setToolTipText(null);
						treeViewer.getControl().setEnabled(true);
					}
				});
				updater.refresh();
			}
		});
		populationThread.setName("Tree population thread");
		populationThread.start();
	}
	
	public void dispose() {
		space.removeChangeListener(spaceListener);
		populationThread.interrupt();
	}
	
	public IShadowEntity getRootEntity() {
		return treeBuilder.getRoot();
	}
	
	public TreeBuilder getTreeBuilder() {
		return treeBuilder;
	}
	
	public ISpace getSpace() {
		return space;
	}
	
	private IEventHandler createSpaceListener() {
		return new IEventHandler() {
			public void handleEvent(final IEvent event) {
				if(event instanceof ISpaceContentChangeEvent) {
					handleSpaceChange((ISpaceContentChangeEvent)event);
				}
			}
		};
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
	
	public List<ISemanticLayer> getLayers() {
		return treeBuilder.getLayers();
	}
	
	public void addLayer(ISemanticLayer layerProvider) {
		treeBuilder.addLayer(layerProvider);
		layersChanged();
	}
	
	public void removeLayer(ISemanticLayer layerProvider) {
		treeBuilder.removeLayer(layerProvider);
		layersChanged();
	}
	
	private void layersChanged() {
		treeBuilder.setRoot(space.getRootEntity());
		for (IEntity entity: space) {
			treeBuilder.addEntity(entity);
		}
		updater.refresh();
	}
}
