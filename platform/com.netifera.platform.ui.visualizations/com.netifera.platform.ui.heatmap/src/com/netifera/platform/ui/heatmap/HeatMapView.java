package com.netifera.platform.ui.heatmap;


import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
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
import org.eclipse.swt.widgets.Shell;
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
import com.netifera.platform.api.model.events.ISpaceContentChangeEvent;
import com.netifera.platform.api.model.layers.ISemanticLayer;
import com.netifera.platform.net.model.HostEntity;
import com.netifera.platform.net.model.InternetAddressEntity;
import com.netifera.platform.ui.internal.heatmap.Activator;
import com.netifera.platform.ui.spaces.SpaceEditorInput;
import com.netifera.platform.ui.spaces.editor.actions.ChooseLayerAction;
import com.netifera.platform.ui.spaces.hover.ActionHover;
import com.netifera.platform.ui.util.MouseTracker;
import com.netifera.platform.util.addresses.inet.IPv4Address;
import com.netifera.platform.util.addresses.inet.InternetAddress;

public class HeatMapView extends ViewPart {

	public static final String ID = "com.netifera.platform.views.heatmap";

	private IMemento memento;
	
	private ISpace space;
	
	private IEventHandler spaceChangeListener = new IEventHandler() {
		public void handleEvent(IEvent event) {
			if(event instanceof ISpaceContentChangeEvent) {
				handleSpaceChange((ISpaceContentChangeEvent)event);
			}
		}
	};

	private HeatMapControl control;
	private HeatMapUpdater updater;
	private Job loadJob;
	
	@Override
	public void createPartControl(final Composite parent) {
		control = new HeatMapControl(parent, SWT.BORDER);
		control.setLayout(new FillLayout());

		updater = HeatMapUpdater.get(control);
		
		if (memento != null) {
			IMemento heatMapMemento = memento.getChild("HeatMap");
			if (heatMapMemento != null)
				control.restoreState(heatMapMemento);
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
						setSpace(newSpace);
					}
				}
				public void partBroughtToTop(IWorkbenchPart part) {
				}
				public void partClosed(IWorkbenchPart part) {
					if (!(part instanceof IEditorPart))
						return;
					IEditorInput editorInput = ((IEditorPart) part).getEditorInput();
					if (editorInput instanceof SpaceEditorInput) {
						ISpace closedSpace = ((SpaceEditorInput)editorInput).getSpace();
						if (closedSpace == HeatMapView.this.space)
							setSpace(null);
					}
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
				HeatMap item = control.getItem(point);
				if (item == null)
					return null;
				
				List<HeatMap> selection = control.getSelection();
				if (selection.contains(item)) {
					if (selection.size() > 1)
						return null; // multiple netblocks not yet implemented
					return item.getNetblock();
				} else {
	//				if (item.size() != 1)
	//					return null;
		
					for (IEntity entity: item)
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
				}
				return null;
			}

			@Override
			protected Rectangle getAreaOfItemAt(Point point) {
				Rectangle itemArea = control.getItemBounds(control.getItem(point));
				if (itemArea != null) {
					return itemArea;
//					return expandedItemArea(itemArea);
				}
				return super.getAreaOfItemAt(point);
			}
			
			@Override
			protected Rectangle getAreaOfSelectedItem() {
				List<HeatMap> selection = control.getSelection();
				if(selection != null && selection.size() > 0) {
					return control.getItemBounds(selection.get(0));
				}
				return null;
			}
			
			@Override
			protected void showInformationControl(Shell parent, Point location,
					Object input, Object item) {
				informationControl = new ActionHover(parent, location, input, item);
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
					if (layerProvider instanceof IHeatMapLayer)
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
				control.setLayer((IHeatMapLayer)provider);
			}
		});
	}

	@Override
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);
		this.memento = memento;
	}

	@Override
	public void dispose() {
		if (this.space != null) {
			this.space.removeChangeListener(spaceChangeListener);
			this.loadJob.cancel();
		}
		
		super.dispose();
	}

	@Override
	public void saveState(IMemento memento) {
		super.saveState(memento);
		IMemento heatMapMemento = memento.createChild("HeatMap");
		control.saveState(heatMapMemento);
	}
	
	@Override
	public void setFocus() {
		// TODO Auto-generated method stub
	}
	
	private void setSpace(final ISpace space) {
		if (this.space != null) {
			if (this.space == space)
				return;
			this.space.removeChangeListener(spaceChangeListener);
			this.loadJob.cancel();
			Thread.yield();
		}

		control.reset();

		this.space = space;
		
		if (space != null) {
			setPartName("HeatMap - "+space.getName());//FIXME this is because the name changes and we dont get notified
			
			loadJob = new Job("HeatMap loading space '"+space.getName()+"'") {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					Thread.yield();
					space.addChangeListener(spaceChangeListener);
					monitor.beginTask("Loading entities", space.size());
					for(IEntity entity: space) {
						addEntity(entity);
						monitor.worked(1);
						if (monitor.isCanceled()) {
							return Status.CANCEL_STATUS;
						}
					}
					
					updater.redraw();
					return Status.OK_STATUS;
				}
			};
			loadJob.setPriority(Job.BUILD);
			loadJob.schedule();
		} else {
			setPartName("HeatMap");
		}
	}
	
	private void handleSpaceChange(final ISpaceContentChangeEvent event) {
		if (!(event.getEntity() instanceof AbstractEntity))
			return;
		final AbstractEntity entity = (AbstractEntity)event.getEntity();
		if(event.isEntityAddEvent()) {
			addEntity(entity);
		} else if(event.isEntityUpdateEvent()) {
			updateEntity(entity);
		} else if(event.isEntityRemoveEvent()) {
			removeEntity(entity);
		}
	}

	private synchronized void addEntity(IEntity entity) {
		if (entity instanceof InternetAddressEntity) {
			final InternetAddressEntity addressEntity = (InternetAddressEntity) entity;
			final InternetAddress address = addressEntity.toNetworkAddress();
			if (address instanceof IPv4Address) {
				control.add((IPv4Address)address, addressEntity.getHost());
				updater.redraw();
			}
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
