package com.netifera.platform.ui.spaces.actions;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import com.netifera.platform.api.model.layers.ISemanticLayer;
import com.netifera.platform.ui.internal.spaces.Activator;

public abstract class SelectLayersAction extends Action {
	
	public SelectLayersAction(String text, ImageDescriptor icon) {
		super(text, SWT.DROP_DOWN);
		setImageDescriptor(icon);
	}
	
	public SelectLayersAction() {
		this("Select Layers", Activator.getDefault().getImageCache().getDescriptor("icons/layers_16x16.png"));
	}
	
	@Override
	public void run() {
		final List<ISemanticLayer> activeLayerProviders = getActiveLayers();
        Menu menu = new Menu(Display.getDefault().getActiveShell(), SWT.POP_UP);
		for (final ISemanticLayer provider: getLayers()) {
			if (provider.getName() == null)
				continue;
			MenuItem item = new MenuItem(menu, SWT.CHECK);
			item.setSelection(activeLayerProviders.contains(provider));
	        item.setText(provider.getName());
	        item.addListener(SWT.Selection, new Listener() {
	          public void handleEvent(Event e) {
	        	  if (activeLayerProviders.contains(provider)) {
	        		  disableLayer(provider);
	        	  } else {
	        		  enableLayer(provider);
	        	  }
	          }
	        });
		}
        menu.setLocation(Display.getDefault().getCursorLocation());
        menu.setVisible(true);
	}
	
	protected abstract List<ISemanticLayer> getLayers();
	protected abstract List<ISemanticLayer> getActiveLayers();
	protected abstract void enableLayer(ISemanticLayer provider);
	protected abstract void disableLayer(ISemanticLayer provider);
}
