package com.netifera.platform.ui.treemap;


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
import com.netifera.platform.api.model.layers.IGroupLayerProvider;
import com.netifera.platform.api.model.layers.ILayerProvider;
import com.netifera.platform.net.model.HostEntity;
import com.netifera.platform.net.model.InternetAddressEntity;
import com.netifera.platform.ui.spaces.SpaceEditorInput;
import com.netifera.platform.util.addresses.inet.IPv4Address;
import com.netifera.platform.util.addresses.inet.InternetAddress;

public class TreeMapView extends ViewPart {

	public static final String ID = "com.netifera.platform.views.treemap";

	private List<ILayerProvider> layerProviders = new ArrayList<ILayerProvider>();
	private IGroupLayerProvider colorLayerProvider;

	private ISpace space;
	private IEventHandler spaceChangeListener;

	private TreeMapWidget treeMapWidget;
	
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

	
	public TreeMapView() {
/*		for (ILayerProvider layerProvider: Activator.getDefault().getModel().getLayerProviders()) {
			if (layerProvider.isDefaultEnabled() &&
					(layerProvider instanceof IGeographicalLayerProvider || layerProvider instanceof IEdgeLayerProvider))
				layerProviders.add(layerProvider);
		}
*/	}

	/**
	 * This is a callback that will allow us to create the viewer and initialize
	 * it.
	 */
	@Override
	public void createPartControl(final Composite parent) {
		
		treeMapWidget = new TreeMapWidget(parent, SWT.BORDER);
		treeMapWidget.setLayout(new FillLayout());
		
		IPageListener pageListener = new IPageListener() {
			IPartListener partListener = new IPartListener() {
				public void partActivated(IWorkbenchPart part) {
					if (!(part instanceof IEditorPart))
						return;
					IEditorInput editorInput = ((IEditorPart) part).getEditorInput();
					if (editorInput instanceof SpaceEditorInput) {
						ISpace newSpace = ((SpaceEditorInput)editorInput).getSpace();
						setPartName(newSpace.getName());//FIXME this is because the name changes and we dont get notified
						if (newSpace != TreeMapView.this.space)
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
//								focusEntity((IEntity)o);
							}
						}
					}
					
				});
/*		IWorkbenchPage page = getSite().getWorkbenchWindow().getActivePage();
		if (page == null)
			return;
*/	
	
		initializeToolBar();
	}

	private void initializeToolBar() {
/*		IToolBarManager toolbarManager = getViewSite().getActionBars()
				.getToolBarManager();
		toolbarManager.add(new SelectLayersAction() {
			@Override
			protected void disableLayer(ILayerProvider provider) {
				removeLayer(provider);
			}
			@Override
			protected void enableLayer(ILayerProvider provider) {
				addLayer(provider);
			}
			@Override
			protected List<ILayerProvider> getActiveLayers() {
				return WorldView.this.getLayers();
			}
			@Override
			protected List<ILayerProvider> getLayers() {
				List<ILayerProvider> answer = new ArrayList<ILayerProvider>();
				for (ILayerProvider layerProvider: Activator.getDefault().getModel().getLayerProviders()) {
					if (layerProvider instanceof IGeographicalLayerProvider || layerProvider instanceof IEdgeLayerProvider)
						answer.add(layerProvider);
				}
				return answer;
			}
		});
		
		toolbarManager.add(new ChooseLayerAction("Set Color", Activator.getDefault().getImageCache().getDescriptor("icons/colors.png")) {
			@Override
			protected List<ILayerProvider> getLayers() {
				List<ILayerProvider> answer = new ArrayList<ILayerProvider>();
				for (ILayerProvider layerProvider: Activator.getDefault().getModel().getLayerProviders()) {
					if (layerProvider instanceof IGroupLayerProvider)
						answer.add(layerProvider);
				}
				return answer;
			}
			@Override
			protected ILayerProvider getActiveLayer() {
				return getColorLayer();
			}
			@Override
			protected void setActiveLayer(ILayerProvider provider) {
				setColorLayer((IGroupLayerProvider)provider);
			}
		});
		
		toolbarManager.add(new ToggleOverviewAction(this));
		toolbarManager.add(new ToggleLabelsAction(this));
		toolbarManager.add(new TogglePlaceNamesAction(this));
		toolbarManager.add(new ToggleFollowNewEntitiesAction(this));
*/	}

	@Override
	public void setFocus() {
		// TODO Auto-generated method stub
	}
	
	private void setSpace(ISpace space) {
		if (this.space != null)
			this.space.removeChangeListener(spaceChangeListener);

		treeMapWidget.reset();

		this.space = space;
		spaceChangeListener = new IEventHandler() {
			public void handleEvent(IEvent event) {
				if(event instanceof ISpaceContentChangeEvent) {
					handleSpaceChange((ISpaceContentChangeEvent)event);
				}
			}
		};
		
		if (space != null) {
			space.addChangeListenerAndPopulate(spaceChangeListener);
			setPartName(space.getName());
		} else {
			setPartName("TreeMapView");
		}
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
		if (entity instanceof InternetAddressEntity) {
			final InternetAddressEntity addressEntity = (InternetAddressEntity) entity;
			final InternetAddress address = addressEntity.getAddress();
			if (address instanceof IPv4Address)
				Display.getDefault().syncExec(new Runnable() {
					public void run() {
						treeMapWidget.add((IPv4Address)address, addressEntity.getHost());
					}
				});
		} else if (entity instanceof HostEntity) {
			HostEntity hostEntity = (HostEntity) entity;
			for (IEntity addressEntity: hostEntity.getAddresses())
				addEntity(addressEntity);
		}
	}

	private synchronized void updateEntity(IEntity entity) {
	}
	
	private synchronized void removeEntity(IEntity entity) {
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

}
