package com.netifera.platform.api.model.events;


/*
 * Fired when spaces are opened or closed
 */
public interface ISpaceStatusChangeEvent extends ISpaceEvent {
	boolean isOpenEvent();
	boolean isCloseEvent();
}
