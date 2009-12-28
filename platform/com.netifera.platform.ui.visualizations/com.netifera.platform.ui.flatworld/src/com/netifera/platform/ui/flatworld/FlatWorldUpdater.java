package com.netifera.platform.ui.flatworld;

import com.netifera.platform.ui.updater.ControlUpdater;

public class FlatWorldUpdater extends ControlUpdater {
	private final FlatWorld control;
	private volatile boolean redraw;

	/*
	 * Updater must be created with this method to avoid creating more than one
	 * wrapper for the same control
	 */
	public static FlatWorldUpdater get(FlatWorld control) {
		ControlUpdater controlUpdater = ControlUpdater.get(control);

		/* return existing updater */
		if(controlUpdater instanceof FlatWorldUpdater) {
			return (FlatWorldUpdater)controlUpdater;
		}
		/* the existing updater is of different class */
		if(controlUpdater != null) {
			throw new IllegalArgumentException("The control has a registered updater of different class.");
		}
		
		controlUpdater = new FlatWorldUpdater(control);
		
		return (FlatWorldUpdater)controlUpdater;
	}

	protected FlatWorldUpdater(FlatWorld control) {
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
