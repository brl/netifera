package com.netifera.platform.ui.treemap;


import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
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
import com.netifera.platform.api.model.layers.ISemanticLayer;
import com.netifera.platform.net.model.HostEntity;
import com.netifera.platform.net.model.InternetAddressEntity;
import com.netifera.platform.ui.internal.treemap.Activator;
import com.netifera.platform.ui.spaces.SpaceEditorInput;
import com.netifera.platform.ui.spaces.editor.actions.ChooseLayerAction;
import com.netifera.platform.ui.spaces.editor.actions.EntityHover;
import com.netifera.platform.ui.util.MouseTracker;
import com.netifera.platform.util.addresses.inet.IPv4Address;
import com.netifera.platform.util.addresses.inet.InternetAddress;

public class TreeMapView extends ViewPart {

	public static final String ID = "com.netifera.platform.views.treemap";

	private ISpace space;
	private IEventHandler spaceChangeListener;

	private TreeMapControl control;
	
	@Override
	public void createPartControl(final Composite parent) {
		control = new TreeMapControl(parent, SWT.BORDER);
		control.setLayout(new FillLayout());
		
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
								focusEntity((IEntity)o);
							}
						}
					}
					
				});
		
/*		IWorkbenchPage page = getSite().getWorkbenchWindow().getActivePage();
		if (page == null)
			return;
*/	
	
		initializeToolBar();
		
		/* implement the mouse tracker the action hover handlers*/
		final MouseTracker mouseTracker = new MouseTracker(control) {
			private PopupDialog informationControl;

			@Override
			protected Object getItemAt(Point point) {
				TreeMap subtree = control.getItem(point);
				if (subtree == null)
					return null;
//				if (subtree.size() != 1)
//					return null;
//				IShadowEntity targetEntity = (IShadowEntity)subtree.getData();
	
				for (IEntity entity: subtree)
					return entity;
				
/*				IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
				List<IShadowEntity> selectionList = selection.toList();
				if (selectionList.contains(targetEntity) && selectionList.size()>1) {
					FolderEntity folder = new FolderEntity(targetEntity.getRealmId(), null, "Selection");
					IShadowEntity folderShadow = TreeStructureContext.createRoot(folder);
					for (IShadowEntity entity: selectionList)
						((TreeStructureContext)folderShadow.getStructureContext()).addChild(entity);
					return folderShadow;
				}
*/				
				return null;
			}

			@Override
			protected Rectangle getAreaOfItemAt(Point point) {
				Rectangle subtreeArea = control.getItemBounds(control.getItem(point));
				if (subtreeArea != null) {
					return subtreeArea;
//					return expandedItemArea(subtreeArea);
				}
				return super.getAreaOfItemAt(point);
			}
			
			@Override
			protected Rectangle getAreaOfSelectedItem() {
				List<TreeMap> selection = control.getSelection();
				if(selection != null && selection.size() > 0) {
					return control.getItemBounds(selection.get(0));
				}
				return null;
			}
			
			private Rectangle expandedItemArea(Rectangle itemArea) {
				return new Rectangle(Math.max(itemArea.x - 12, 2), Math.max(itemArea.y
						- EPSILON * 2, 0), itemArea.width + 12 * 2, itemArea.height
						+ EPSILON * 2 * 2);
			}
			
			@Override
			protected void showInformationControl(Shell parent, Point location,
					Object input, Object item) {
				informationControl = new EntityHover(parent, location, input, item);
				informationControl.open();
			}
			@Override
			protected void hideInformationControl() {
				if(informationControl != null) {
					informationControl.close();
				}
			}
			@Override
			protected boolean focusInformationControl() {
				if(informationControl != null) {
					Shell shell = informationControl.getShell();
					if(shell != null) {
						return shell.setFocus();
					}
				}
				return false;
			}
			
			@Override
			protected Rectangle getInformationControlArea() {
				if(informationControl != null) {
					Shell shell = informationControl.getShell();
					if(shell != null) {
						return shell.getBounds();
					}
				}
				return null;
			}
			
			@Override
			public Object getInput() {
				return space;
			}
		};
		
		control.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent event) {
				mouseTracker.stop();
			}
		});
	}

	private void initializeToolBar() {
		IToolBarManager toolbarManager = getViewSite().getActionBars().getToolBarManager();
		
		toolbarManager.add(new ChooseLayerAction() {
			@Override
			protected List<ISemanticLayer> getLayers() {
				List<ISemanticLayer> answer = new ArrayList<ISemanticLayer>();
				for (ISemanticLayer layerProvider: Activator.getInstance().getModel().getSemanticLayers()) {
					if (layerProvider instanceof ITreeMapLayer)
						answer.add(layerProvider);
				}
				return answer;
			}
			@Override
			protected ISemanticLayer getActiveLayer() {
				return control.getLayer();
			}
			@Override
			protected void setActiveLayer(ISemanticLayer provider) {
				control.setLayer((ITreeMapLayer)provider);
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

		control.reset();

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
						control.add((IPv4Address)address, addressEntity.getHost());
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

	private void focusEntity(IEntity o) {
		// TODO Auto-generated method stub
	}
}
