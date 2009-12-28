package com.netifera.platform.ui.spaces.graphs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.viewers.Viewer;

import prefuse.data.Graph;
import prefuse.data.Node;
import prefuse.data.Schema;

import com.netifera.platform.api.events.IEvent;
import com.netifera.platform.api.events.IEventHandler;
import com.netifera.platform.api.model.IEntity;
import com.netifera.platform.api.model.ISpace;
import com.netifera.platform.api.model.events.ISpaceContentChangeEvent;
import com.netifera.platform.api.model.layers.IEdge;
import com.netifera.platform.api.model.layers.IEdgeLayer;
import com.netifera.platform.api.model.layers.IGroupLayer;
import com.netifera.platform.api.model.layers.ISemanticLayer;
import com.netifera.platform.api.model.layers.ITreeLayer;
import com.netifera.platform.ui.graphs.GraphViewer;
import com.netifera.platform.ui.graphs.IGraphContentProvider;


public class SpaceGraphContentProvider implements IGraphContentProvider {
	private static final boolean DEBUG_CONTENT_PROVIDER = false;

	private GraphViewer viewer;
	private IEventHandler spaceListener;
	private ISpace space;

	private List<ISemanticLayer> layerProviders = new ArrayList<ISemanticLayer>();

	private Graph graph;
	private Map<IEntity, Node> nodeMap;
	private volatile IGroupLayer colorLayerProvider;
	private volatile IGroupLayer shapeLayerProvider;

	static private Schema schema;
	static public synchronized Schema getNodeSchema() {
		if (schema == null) {
			schema = new Schema();
			schema.addColumn("entity", IEntity.class, null);
			schema.addColumn("type", String.class, null);
			schema.addColumn("realm", Long.class, null);
			schema.addColumn("color", String.class, null);
			schema.addColumn("shape", String.class, null);
		}
		return schema;
	}

	public Graph getGraph() {
		return graph;
	}
	
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if(!validInputChange(viewer, newInput)) {
			return;
		}
		
		if(DEBUG_CONTENT_PROVIDER) {
			debug("inputChanged old = " + oldInput + " new = " + newInput);
		}
		
		this.space = (ISpace) newInput;
		this.viewer = (GraphViewer) viewer;

		initializeGraph();
		
		this.spaceListener = createSpaceListener();
		this.space.addChangeListenerAndPopulate(spaceListener);
	}

	private void initializeGraph() {
		nodeMap = new HashMap<IEntity,Node>();
		graph = new Graph(true); //directed
		graph.getNodeTable().addColumns(getNodeSchema());
	}

	private void updateGraph() {
		synchronized (viewer.getControl().getVisualization()) { //FIXME maybe this will lock too long time
			for (IEntity entity: space)
				try {
					addEntity(entity);
				} catch (Throwable e) {
					e.printStackTrace();
				}

			viewer.update();
		}
	}

	public void dispose() {
		space.removeChangeListener(spaceListener);
	}
	
	private void debug(String message) {
		System.err.println("SpaceGraphContentProvider : " + message);
	}
	
	private boolean validInputChange(Viewer viewer, Object newInput) {
		return (viewer instanceof GraphViewer) && (newInput instanceof ISpace);
	}
	
	private IEventHandler createSpaceListener() {
		return new IEventHandler() {
			public void handleEvent(IEvent event) {
				if(event instanceof ISpaceContentChangeEvent) {
					synchronized (viewer.getControl().getVisualization()) {
						handleSpaceChange((ISpaceContentChangeEvent)event);
					}
				}
			}			
		};
	}
	
	private synchronized void handleSpaceChange(final ISpaceContentChangeEvent event) {
		try {
			if(event.isEntityAddEvent()) {
				addEntity(event.getEntity());
			} else if(event.isEntityUpdateEvent()) {
				addEntity(event.getEntity());
			} else if(event.isEntityRemoveEvent()) {
				removeEntity(event.getEntity());
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	private void addEntity(final IEntity entity) {
		for (ISemanticLayer layerProvider: layerProviders) {
			if (layerProvider instanceof IEdgeLayer) {
				IEdgeLayer edgeLayerProvider = (IEdgeLayer)layerProvider;
				for (IEdge edge: edgeLayerProvider.getEdges(entity)) {
					addEdge(edge);
					viewer.update();
				}
			}
			if (layerProvider instanceof ITreeLayer) {
				ITreeLayer treeLayerProvider = (ITreeLayer)layerProvider;
				for (final IEntity parent: treeLayerProvider.getParents(entity)) {
					addEdge(new IEdge() {
						public IEntity getSource() {
							return parent;
						}
						public IEntity getTarget() {
							return entity;
						}
					});
					viewer.update();
				}
			}
/*			if (layerProvider instanceof IGroupLayerProvider) {
				IGroupLayerProvider groupLayerProvider = (IGroupLayerProvider)layerProvider;
				Node node = null;
				for (String each: groupLayerProvider.getGroups(entity)) {
					if (node == null)
						node = createNode(entity);
					node.set("aggregate", each);
					break;
				}
			}
*/		}
		
		Node node = nodeMap.get(entity);
		if (node != null) {
//			node.set("color", entity.getTypeName());
			node.set("color", null);
			if (colorLayerProvider != null) {
				for (String group: colorLayerProvider.getGroups(entity)) {
					node.set("color", group);
					break;
				}
			}
			node.set("shape", null);
			if (shapeLayerProvider != null) {
				for (String group: shapeLayerProvider.getGroups(entity)) {
					node.set("shape", group);
					break;
				}
			}
		}
	}
	
	private void removeEntity(IEntity entity) {
		if(viewer.getControl().isDisposed())
			return;
		
		Node node = nodeMap.remove(entity);
		if (node != null) {
			graph.removeNode(node); //XXX should first remove edges?
		}
		viewer.refresh();
	}
	
	private void addEdge(final IEdge edge) {
		Node srcNode = createNode(edge.getSource());
		Node dstNode = createNode(edge.getTarget());
		
		// dont add edges twice, otherwise the spring layout goes crazy and shaky
		if (graph.getEdge(srcNode, dstNode) == null)
			graph.addEdge(srcNode, dstNode);
	}

	//TODO remove edges when the relationship goes away
	private void removeEdge(final IEntity src, final IEntity dst) {
		graph.removeEdge(graph.getEdge(nodeMap.get(src), nodeMap.get(dst)));
	}

	private Node createNode(IEntity entity) {
		Node node = nodeMap.get(entity);
		if (node == null) {
//			node = (Node) graph.getNodeTable().addTuple(new EntityTuple(entity));
			node = graph.addNode();
			node.set("entity", entity);
			node.set("type", entity.getTypeName());
			node.set("realm", entity.getRealmId());
			nodeMap.put(entity, node);
		}
		return node;
	}
	
/*	private Node createGroupNode(String group, long realm) {
		Node answer = groupNodeMap.get(group);
		if (answer == null) {
			answer = createNode(new FolderEntity(realm, null, group));
			groupNodeMap.put(group, answer);
		}
		return answer;
	}
*/
	public List<ISemanticLayer> getLayers() {
		return layerProviders;
	}
	
	public synchronized void addLayer(ISemanticLayer layerProvider) {
		layerProviders.add(layerProvider);
		updateGraph();
		viewer.update();
	}
	
	public synchronized void removeLayer(ISemanticLayer layerProvider) {
		layerProviders.remove(layerProvider);
		initializeGraph();
		updateGraph();
		viewer.refresh();
	}
	
	public IGroupLayer getColorLayer() {
		return colorLayerProvider;
	}
	
	public synchronized void setColorLayer(IGroupLayer layerProvider) {
		colorLayerProvider = layerProvider;
		if (layerProvider == null)
			initializeGraph();
		updateGraph();
		viewer.update();
	}
	
	public IGroupLayer getShapeLayer() {
		return shapeLayerProvider;
	}
	
	public synchronized void setShapeLayer(IGroupLayer layerProvider) {
		shapeLayerProvider = layerProvider;
		if (layerProvider == null)
			initializeGraph();
		updateGraph();
		viewer.update();
	}
}
