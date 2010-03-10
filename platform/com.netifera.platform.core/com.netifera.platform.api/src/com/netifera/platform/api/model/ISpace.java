package com.netifera.platform.api.model;

import java.util.List;
import java.util.Set;

import com.netifera.platform.api.events.IEventHandler;
import com.netifera.platform.api.tasks.ITaskRecord;
import com.netifera.platform.api.tasks.ITaskStatus;

public interface ISpace extends Iterable<IEntity> {
	long getId();
	IWorkspace getWorkspace();
	
	long getProbeId();
	IEntity getRootEntity();
	boolean isIsolated();

	/*
	 * Open and close spaces. Fire ISpaceStatusChangeEvent.
	 */
	void open();
	void close();
	boolean isOpened();

	/*
	 * Delete a space, closing it first. Fire ISpaceLifecycleEvent.
	 * When isolated spaces are deleted, also all the entities in its realm and sub-realms are deleted from the workspace, as well as sub-spaces.
	 */
	void delete();
	
	String getName();
	
	/*
	 * Rename a space. Fire ISpaceRenameEvent.
	 */
	void setName(String name);
	
	List<IEntity> getEntities();
	int size();
	boolean contains(IEntity entity);
	
	Set<String> getTags();

	/*
	 * Add, update and remove entities from the space. Fire ISpaceContentChangeEvent.
	 */
	void addEntity(IEntity entity);
	void updateEntity(IEntity entity);
	void removeEntity(IEntity entity);

	void addChangeListener(IEventHandler handler);
	void addChangeListenerAndPopulate(IEventHandler handler);
	void removeChangeListener(IEventHandler handler);
	
	ITaskRecord[] getTasks();
	boolean isActive();

	/*
	 * Add new tasks and update existing tasks. Fire ISpaceTaskChangeEvent.
	 */
	void addTask(ITaskStatus record);
	void updateTask(ITaskRecord record);

	void addTaskChangeListener(IEventHandler handler);
	void addTaskChangeListenerAndPopulate(IEventHandler handler);
	void removeTaskChangeListener(IEventHandler handler);
}
