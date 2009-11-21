package com.netifera.platform.ui.flatworld;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPageListener;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.ViewPart;

import com.netifera.platform.api.events.IEvent;
import com.netifera.platform.api.events.IEventHandler;
import com.netifera.platform.api.model.AbstractEntity;
import com.netifera.platform.api.model.IEntity;
import com.netifera.platform.api.model.ISpace;
import com.netifera.platform.api.model.ISpaceContentChangeEvent;
import com.netifera.platform.api.model.layers.IEdge;
import com.netifera.platform.api.model.layers.IEdgeLayerProvider;
import com.netifera.platform.api.model.layers.IGroupLayerProvider;
import com.netifera.platform.api.model.layers.ILayerProvider;
import com.netifera.platform.net.geoip.IGeographicalLayerProvider;
import com.netifera.platform.net.geoip.ILocation;
import com.netifera.platform.ui.internal.flatworld.Activator;
import com.netifera.platform.ui.spaces.SpaceEditorInput;

public class FlatWorldView extends ViewPart {

	public static final String ID = "com.netifera.platform.views.flatworld";

	private FlatWorld world;
	
	private volatile boolean followNewEntities = true;
	private IEntity focusEntity;

	private List<ILayerProvider> layerProviders = new ArrayList<ILayerProvider>();
	private IGroupLayerProvider colorLayerProvider;

	private ISpace space;
	private IEventHandler spaceChangeListener;
	
	static private final long FLY_TIME_MSECS = 2000; // time to fly to focused entity
	
	static private final Color[] palette = new Color[] {
		new Color(255, 255, 150, 192),
		new Color(202, 62, 94, 192),
		new Color(255, 152, 213, 192),
		new Color(83, 140, 208, 192),
		new Color(178, 220, 205, 192),
		new Color(146, 248, 70, 192),
		
		new Color(255,255,0, 192),
		new Color(255,200,47, 192),
		new Color(255,118,0, 192),
		new Color(255,0,0, 192),
		new Color(175,13,102, 192),
		new Color(121,33,135, 192)
		
/*		new Color(0xff, 0xc8, 0x00, 0xc0),
		new Color(0xff, 0x00, 0xc8, 0xc0),
		new Color(0xc8, 0x00, 0xff, 0xc0),
		new Color(0x00, 0xc8, 0xff, 0xc0),
		new Color(0xc8, 0xff, 0x00, 0xc0),
		new Color(0x00, 0xff, 0xc8, 0xc0),

/*		Color.RED,
		Color.GREEN,
		Color.BLUE,
		Color.YELLOW,
		Color.MAGENTA,
		Color.ORANGE,
		Color.CYAN,
		Color.PINK
*/	};


	
	@Override
	public void createPartControl(final Composite parent) {
		for (ILayerProvider layerProvider: Activator.getInstance().getModel().getLayerProviders()) {
			if (layerProvider.isDefaultEnabled() &&
					(layerProvider instanceof IGeographicalLayerProvider || layerProvider instanceof IEdgeLayerProvider))
				layerProviders.add(layerProvider);
		}
		
		world = new FlatWorld(parent, SWT.BORDER);
		world.setLayout(new FillLayout());
		
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
			space.addChangeListenerAndPopulate(spaceChangeListener);
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
		if(event.isCreationEvent()) {
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
		
		for (ILayerProvider layerProvider: layerProviders) {
			if (layerProvider instanceof IEdgeLayerProvider) {
				IEdgeLayerProvider edgeLayerProvider = (IEdgeLayerProvider)layerProvider;
				for (IEdge edge: edgeLayerProvider.getEdges(entity)) {
					addEdge(edge);
				}
			}
		}
		
//		worldWindow.repaint();//XXX is this needed here?
	}

	private synchronized void updateEntity(IEntity entity) {
		// TODO
		for (ILayerProvider layerProvider: layerProviders) {
			if (layerProvider instanceof IEdgeLayerProvider) {
				IEdgeLayerProvider edgeLayerProvider = (IEdgeLayerProvider)layerProvider;
				for (IEdge edge: edgeLayerProvider.getEdges(entity)) {
					addEdge(edge);
				}
			}
		}

		if (entity == focusEntity)
			focusEntity(entity); // refocus, update label
		
//		worldWindow.repaint();//XXX is this needed here?
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
		for (ILayerProvider layerProvider: layerProviders) {
			if (layerProvider instanceof IGeographicalLayerProvider) {
				ILocation location = ((IGeographicalLayerProvider)layerProvider).getLocation(entity);
				if (location != null) {
					return location;
				}
			}
		}
		
		return null;
	}

	public void addLayer(ILayerProvider layerProvider) {
		layerProviders.add(layerProvider);
		setSpace(space);//to repopulate
	}
	
	public void removeLayer(ILayerProvider layerProvider) {
		layerProviders.remove(layerProvider);
		setSpace(space);//to repopulate
	}

	public List<ILayerProvider> getLayers() {
		return layerProviders;
	}
	
	public IGroupLayerProvider getColorLayer() {
		return colorLayerProvider;
	}
	
	public void setColorLayer(IGroupLayerProvider layerProvider) {
		colorLayerProvider = layerProvider;
		setSpace(space);//to repopulate
	}

	public synchronized void setFollowNewEnabled(boolean enabled) {
		followNewEntities = enabled;
	}

	public synchronized boolean isFollowNewEnabled() {
		return followNewEntities;
	}

	public synchronized void focusEntity(IEntity entity) {
	}
}
