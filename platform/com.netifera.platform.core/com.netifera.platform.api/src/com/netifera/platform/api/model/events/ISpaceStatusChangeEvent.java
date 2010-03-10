package com.netifera.platform.api.model.events;


/*
 * Fired when spaces are opened or closed, or become active/inactive (tasks running or not)
 */
public interface ISpaceStatusChangeEvent extends ISpaceEvent {
	boolean isOpenEvent();
	boolean isCloseEvent();
	boolean isActivateEvent();
	boolean isDeactivateEvent();
}
