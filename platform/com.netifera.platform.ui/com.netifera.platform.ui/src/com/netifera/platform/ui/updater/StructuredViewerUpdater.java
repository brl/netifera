package com.netifera.platform.ui.updater;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jface.viewers.StructuredViewer;

public class StructuredViewerUpdater extends ControlUpdater {
	private final StructuredViewer viewer;
	private Object newInput;
	private Object currentInput;
	private volatile boolean refresh;
	private volatile boolean setInput;
	
	// Concurrent not really needed, but seems to be faster than th enormal HashMap
	private Map<Object,Object> refreshElements = new ConcurrentHashMap<Object,Object>();
	private Map<Object,Object> updateElements = new ConcurrentHashMap<Object,Object>();


	/* private constructor to force to use the get method */
	private StructuredViewerUpdater(StructuredViewer viewer) {
		super(viewer.getControl());
		this.viewer = viewer;
		currentInput = viewer.getInput();
	}

	/*
	 * Updater must be created with this method to avoid creating more than one
	 * wrapper for the same viewer control
	 */
	public static StructuredViewerUpdater get(StructuredViewer viewer) {
		ControlUpdater controlUpdater = get(viewer.getControl());

		/* return existing updater */
		if(controlUpdater instanceof StructuredViewerUpdater) {
			return (StructuredViewerUpdater)controlUpdater;
		}
		/* the existing updater is of different class */
		if(controlUpdater != null) {
			throw new IllegalArgumentException("The control has a registered updater of different class.");
		}
		
		controlUpdater = new StructuredViewerUpdater(viewer);
		
		return (StructuredViewerUpdater)controlUpdater;
	}

	/**
	 * updateControl() is synchronized and is executed in the UI thread, content
	 * providers calling ViewerUpdater methods will be blocked while this method
	 * executes. And the UI thread will be blocked while content providers
	 * invoke this updater methods below.
	 */
	protected void updateControl() {
		final Object[] relements;
		final Object[] uelements;
		synchronized(this) {
			if(checkDisposed()) {
				return;
			}
			/* .setInput() */
			if (setInput) {
				currentInput = newInput;
				newInput = null;
				setInput = false;
				viewer.setInput(currentInput);
			}

			/* .refresh() */
			if(refresh) {
				refresh = false;
				viewer.refresh();
			}
			relements = refreshElements.values().toArray();
			refreshElements.clear();
			uelements = updateElements.values().toArray();
			updateElements.clear();
		}
		/* iterate outside the synchronized block, it makes sense if calling refresh
		 * is slower than copying the values to the array */
		for(Object element : relements) {
			viewer.refresh(element);
		}
		
		for(Object element : uelements) {
			viewer.update(element,null);
		}
	}
	
	/* the following methods are called from content providers */

	public synchronized void refresh() {
		refresh = true;
		updateElements.clear();
		refreshElements.clear();
		scheduleUpdate();
	}
	
	public synchronized void refresh(Object element) {
		if(element != null) {
			/* the map is used as a set, but we need the newest instance of element */
			refreshElements.put(element, element);
			scheduleUpdate();
		} else {
			/*refresh all if element is null hack?*/
			refresh();
		}
	}
	
	public synchronized void update(Object element) {
		if(element != null) {
			/* the map is used as a set, but we need the newest instance of element */
			updateElements.put(element, element);
			scheduleUpdate();
		}
	}
	
	//XXX properties are being ignored
	public void update(Object element, String prop[]) {
		update(element);
	}
	
	public synchronized void setInput(Object input) {
		if (input == null || !input.equals(this.currentInput)) {
			newInput = input;
			setInput = true;
			refresh();
		}
	}
		
	public String toString() {
		return "StructuredViewerUpdater ("+updateElements.size()+", "+refreshElements.size()+")";
	}
}
