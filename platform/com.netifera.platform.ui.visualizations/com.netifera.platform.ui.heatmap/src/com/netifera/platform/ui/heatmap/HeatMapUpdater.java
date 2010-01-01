package com.netifera.platform.ui.heatmap;

import com.netifera.platform.ui.updater.ControlUpdater;

public class HeatMapUpdater extends ControlUpdater {
	private final HeatMapControl control;
	private volatile boolean redraw;

	/*
	 * Updater must be created with this method to avoid creating more than one
	 * wrapper for the same control
	 */
	public static HeatMapUpdater get(HeatMapControl control) {
		ControlUpdater controlUpdater = ControlUpdater.get(control);

		/* return existing updater */
		if(controlUpdater instanceof HeatMapUpdater) {
			return (HeatMapUpdater)controlUpdater;
		}
		/* the existing updater is of different class */
		if(controlUpdater != null) {
			throw new IllegalArgumentException("The control has a registered updater of different class.");
		}
		
		controlUpdater = new HeatMapUpdater(control);
		
		return (HeatMapUpdater)controlUpdater;
	}

	protected HeatMapUpdater(HeatMapControl control) {
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
