package com.netifera.platform.api.model.events;

/*
 * Fired when spaces are created or deleted
 */
public interface ISpaceLifecycleEvent extends ISpaceEvent {
	boolean isCreateEvent();
	boolean isDeleteEvent();
}
