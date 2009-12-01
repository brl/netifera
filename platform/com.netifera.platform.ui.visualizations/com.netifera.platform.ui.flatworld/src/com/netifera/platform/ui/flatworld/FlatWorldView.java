package com.netifera.platform.ui.flatworld;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPageListener;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

import com.netifera.platform.api.events.IEvent;
import com.netifera.platform.api.events.IEventHandler;
import com.netifera.platform.api.model.AbstractEntity;
import com.netifera.platform.api.model.IEntity;
import com.netifera.platform.api.model.ISpace;
import com.netifera.platform.api.model.ISpaceContentChangeEvent;
import com.netifera.platform.api.model.layers.IEdge;
import com.netifera.platform.api.model.layers.IEdgeLayer;
import com.netifera.platform.api.model.layers.ISemanticLayer;
import com.netifera.platform.net.geoip.IGeographicalLayer;
import com.netifera.platform.net.geoip.ILocation;
import com.netifera.platform.ui.internal.flatworld.Activator;
import com.netifera.platform.ui.spaces.SpaceEditorInput;

public class FlatWorldView extends ViewPart {

	public static final String ID = "com.netifera.platform.views.flatworld";

	private IMemento memento;
	
	private FlatWorld world;
	
	private volatile boolean followNewEntities = false;
	private IEntity focusEntity;

	private List<ISemanticLayer> layers = new ArrayList<ISemanticLayer>();

	private ISpace space;
	private IEventHandler spaceChangeListener;
	
	@Override
	public void createPartControl(final Composite parent) {
		for (ISemanticLayer layer: Activator.getInstance().getModel().getSemanticLayers()) {
			if (layer.isDefaultEnabled() &&
					(layer instanceof IGeographicalLayer || layer instanceof IEdgeLayer))
				layers.add(layer);
		}
		
		world = new FlatWorld(parent, SWT.BORDER);
		world.setLayout(new FillLayout());

		if (memento != null) {
			IMemento worldMemento = memento.getChild("World");
			if (worldMemento != null)
				world.restoreState(worldMemento);
			memento = null;
		}

		IPageListener pageListener = new IPageListener() {
			IPartListener partListener = new IPartListener() {
				public void partActivated(IWorkbenchPart part) {
					if (!(part instanceof IEditorPart))
						return;
					IEditorInput editorInput = ((IEditorPart) part).getEditorInput();
					if (editorInput instanceof SpaceEditorInput) {
						ISpace newSpace = ((SpaceEditorInput)editorInput).getSpace();
						setPartName(newSpace.getName());//FIXME this is because the name changes and we dont get notified
						if (newSpace != FlatWorldView.this.space)
							setSpace(newSpace);
					}
				}
				public void partBroughtToTop(IWorkbenchPart part) {
				}
				public void partClosed(IWorkbenchPart part) {
				}
				public void partDeactivated(IWorkbenchPart part) {
				}
				public void partOpened(IWorkbenchPart part) {
				}
			};
			public void pageActivated(IWorkbenchPage page) {
				page.addPartListener(partListener);
				IEditorPart editor = page.getActiveEditor();
				if (editor != null) partListener.partActivated(editor);
			}
			public void pageClosed(IWorkbenchPage page) {
				page.removePartListener(partListener);
			}
			public void pageOpened(IWorkbenchPage page) {
			}
		};
		
		getSite().getWorkbenchWindow().addPageListener(pageListener);
		IWorkbenchPage page = getSite().getWorkbenchWindow().getActivePage();
		if (page != null)
			pageListener.pageActivated(page);

		getSite().getWorkbenchWindow().getSelectionService().addPostSelectionListener(
				//"com.netifera.platform.editors.spaces",
				new ISelectionListener() {
					public void selectionChanged(IWorkbenchPart part, org.eclipse.jface.viewers.ISelection sel) {
						if(sel instanceof IStructuredSelection && !sel.isEmpty()) {
							Object o = ((IStructuredSelection)sel).iterator().next();
							if(o instanceof IEntity) {
								focusEntity((IEntity)o);
							}
						}
					}
					
				});
/*		IWorkbenchPage page = getSite().getWorkbenchWindow().getActivePage();
		if (page == null)
			return;
*/	
	
//		initializeToolBar();
	}

	@Override
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);
		this.memento = memento;
	}

	@Override
	public void saveState(IMemento memento) {
		super.saveState(memento);
		IMemento worldMemento = memento.createChild("World");
		world.saveState(worldMemento);
	}
	
	@Override
	public void setFocus() {
		// TODO Auto-generated method stub
	}

	private void setSpace(ISpace space) {
		if (this.space != null)
			this.space.removeChangeListener(spaceChangeListener);

		world.initializeLayers();
		
		this.space = space;
		spaceChangeListener = new IEventHandler() {
			public void handleEvent(final IEvent event) {
				if(event instanceof ISpaceContentChangeEvent) {
					handleSpaceChange((ISpaceContentChangeEvent)event);
				}
			}
		};
		if (space != null) {
			world.setRandropsEnabled(false);
			space.addChangeListenerAndPopulate(spaceChangeListener);
			world.setRandropsEnabled(true);
			setPartName(space.getName());
		} else {
			setPartName("World");
		}
		
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				world.redraw();
			}
		});
	}
	
	private void handleSpaceChange(final ISpaceContentChangeEvent event) {
		if (!(event.getEntity() instanceof AbstractEntity))
			return;
		final AbstractEntity entity = (AbstractEntity)event.getEntity();
		if(event.isAdditionEvent()) {
			addEntity(entity);
		} else if(event.isUpdateEvent()) {
			updateEntity(entity);
		} else if(event.isRemovalEvent()) {
			removeEntity(entity);
		}
	}

	private synchronized void addEntity(IEntity entity) {
		if (entity.getTypeName().equals("host")) {
			addNode(entity);
			if (followNewEntities) {
				focusEntity(entity);
			}
		}
		
		for (ISemanticLayer layer: layers) {
			if (layer instanceof IEdgeLayer) {
				IEdgeLayer edgeLayer = (IEdgeLayer)layer;
				for (IEdge edge: edgeLayer.getEdges(entity)) {
					addEdge(edge);
				}
			}
		}
	}

	private synchronized void updateEntity(IEntity entity) {
		// TODO
		for (ISemanticLayer layer: layers) {
			if (layer instanceof IEdgeLayer) {
				IEdgeLayer edgeLayer = (IEdgeLayer)layer;
				for (IEdge edge: edgeLayer.getEdges(entity)) {
					addEdge(edge);
				}
			}
		}

		if (entity == focusEntity)
			focusEntity(entity); // refocus, update label
	}
	
	private synchronized void removeEntity(IEntity entity) {
	}

	private void addNode(IEntity entity) {
		final ILocation location = getLocation(entity);
		if (location != null) {
			final String label = location.getCity() != null ? location.getCity() : location.getCountry();
			if (label != null) {
				Display.getDefault().syncExec(new Runnable() {
					public void run() {
						world.addLabel(location.getPosition()[0], location.getPosition()[1], label);
						world.addRaindrop(location.getPosition()[0], location.getPosition()[1], Display.getDefault().getSystemColor(SWT.COLOR_RED));
					}
				});
			}
		}
	}

	private void addEdge(IEdge edge) {
/*		ILocation sourceLocation = getLocation(edge.getSource());
		ILocation targetLocation = getLocation(edge.getTarget());
		if (sourceLocation != null && targetLocation != null)
			worldWidget.addLine(sourceLocation.getPosition()[0], sourceLocation.getPosition()[1], targetLocation.getPosition()[0], targetLocation.getPosition()[1]);
*/	}

	private ILocation getLocation(IEntity entity) {
		for (ISemanticLayer layer: layers) {
			if (layer instanceof IGeographicalLayer) {
				ILocation location = ((IGeographicalLayer)layer).getLocation(entity);
				if (location != null) {
					return location;
				}
			}
		}
		
		return null;
	}

	public synchronized void focusEntity(IEntity entity) {
		final ILocation location = getLocation(entity);
			Display.getDefault().syncExec(new Runnable() {
				public void run() {
					if (location != null) {
						world.setFocus(location.getPosition()[0], location.getPosition()[1]);
					} else {
						world.unsetFocus();
					}
				}
			});
	}
}
