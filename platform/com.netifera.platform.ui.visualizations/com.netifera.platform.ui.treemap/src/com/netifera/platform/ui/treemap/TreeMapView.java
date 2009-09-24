package com.netifera.platform.ui.treemap;


import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
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
import com.netifera.platform.ui.spaces.actions.ChooseLayerAction;
import com.netifera.platform.util.addresses.inet.IPv4Address;
import com.netifera.platform.util.addresses.inet.InternetAddress;

public class TreeMapView extends ViewPart {

	public static final String ID = "com.netifera.platform.views.treemap";

	private IGroupLayerProvider colorLayerProvider;

	private ISpace space;
	private IEventHandler spaceChangeListener;

	private TreeMapWidget treeMapWidget;
	
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
		IToolBarManager toolbarManager = getViewSite().getActionBars().getToolBarManager();
		
		toolbarManager.add(new ChooseLayerAction("Set Color", Activator.getInstance().getImageCache().getDescriptor("icons/colors.png")) {
			@Override
			protected List<ILayerProvider> getLayers() {
				List<ILayerProvider> answer = new ArrayList<ILayerProvider>();
				for (ILayerProvider layerProvider: Activator.getInstance().getModel().getLayerProviders()) {
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
	}

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

	public IGroupLayerProvider getColorLayer() {
		return colorLayerProvider;
	}
	
	public void setColorLayer(IGroupLayerProvider layerProvider) {
		colorLayerProvider = layerProvider;
		treeMapWidget.setColorProvider(new IColorProvider() {
			Color[] palette = {Display.getDefault().getSystemColor(SWT.COLOR_BLUE),
					Display.getDefault().getSystemColor(SWT.COLOR_GREEN),
					Display.getDefault().getSystemColor(SWT.COLOR_YELLOW),
					Display.getDefault().getSystemColor(SWT.COLOR_RED)};
			
			public Color getBackground(Object element) {
				return getForeground(element);
			}

			public Color getForeground(Object element) {
				for (String group: colorLayerProvider.getGroups((IEntity)element)) {
					int v = group.hashCode();
					if (v < 0) v = -v;
					return palette[v % palette.length];
				}
				return Display.getDefault().getSystemColor(SWT.COLOR_BLACK);
			}
		});
		setSpace(space);//to repopulate
	}
}
