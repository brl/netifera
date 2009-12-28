package com.netifera.platform.ui.treemap;

import com.netifera.platform.ui.updater.ControlUpdater;

public class TreeMapUpdater extends ControlUpdater {
	private final TreeMapControl control;
	private volatile boolean redraw;

	/*
	 * Updater must be created with this method to avoid creating more than one
	 * wrapper for the same control
	 */
	public static TreeMapUpdater get(TreeMapControl control) {
		ControlUpdater controlUpdater = ControlUpdater.get(control);

		/* return existing updater */
		if(controlUpdater instanceof TreeMapUpdater) {
			return (TreeMapUpdater)controlUpdater;
		}
		/* the existing updater is of different class */
		if(controlUpdater != null) {
			throw new IllegalArgumentException("The control has a registered updater of different class.");
		}
		
		controlUpdater = new TreeMapUpdater(control);
		
		return (TreeMapUpdater)controlUpdater;
	}

	protected TreeMapUpdater(TreeMapControl control) {
		super(control);
		this.control = control;
	}

	@Override
	protected void updateControl() {
		if (redraw) {
			redraw = false;
			control.redraw();
		}
	}
	
	public void redraw() {
		redraw = true;
		scheduleUpdate();
	}
}
