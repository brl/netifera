package com.netifera.platform.internal.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.db4o.ObjectContainer;
import com.netifera.platform.api.events.EventListenerManager;
import com.netifera.platform.api.events.IEventHandler;
import com.netifera.platform.api.log.ILogger;
import com.netifera.platform.api.model.AbstractEntity;
import com.netifera.platform.api.model.IEntity;
import com.netifera.platform.api.model.ISpace;
import com.netifera.platform.api.model.events.ISpaceContentChangeEvent;
import com.netifera.platform.api.probe.IProbe;
import com.netifera.platform.api.tasks.ITaskRecord;
import com.netifera.platform.internal.model.events.SpaceChangeEvent;
import com.netifera.platform.internal.model.events.SpaceCreateEvent;
import com.netifera.platform.internal.model.events.SpaceDeleteEvent;
import com.netifera.platform.internal.model.events.SpaceOpenCloseEvent;
import com.netifera.platform.model.SpaceEntity;

public class SpaceManager {
	private long currentSpaceId;
	private transient Set<ISpace> openSpaces;
	private Set<ISpace> allSpaces;
	private Map <Long, ISpace> taskIdToSpace;
	private Map <Long, ISpace> spaceIdToSpace;
	private transient ObjectContainer database;
	private transient Workspace workspace;
	private transient EventListenerManager changeListeners;
	private transient ILogger logger;

	public static SpaceManager getSpaceManager(final ObjectContainer db, final Workspace workspace) {
		final List<SpaceManager> result = db.query(SpaceManager.class);
		if(result.isEmpty()) {
			return new SpaceManager(db, workspace);
		} else if(result.size() == 1) {
			return result.get(0).initialize(db, workspace);
		} else {
			throw new IllegalStateException("Multiple SpaceManager objects found in database");
		}
	}
	
	ObjectContainer getDatabase() {
		return database;
	}
	
	private SpaceManager(ObjectContainer db, Workspace workspace) {
		initialize(db, workspace);
		allSpaces = new HashSet<ISpace>();
		taskIdToSpace = new HashMap<Long, ISpace>();
		spaceIdToSpace = new HashMap<Long, ISpace>();
		commit();
	}	

	private SpaceManager initialize(ObjectContainer db, Workspace workspace) {
		this.database = db;
		this.workspace = workspace;
		this.openSpaces = new HashSet<ISpace>();
		if(workspace != null) {
			logger = workspace.getLogger();
		}
		return this;
	}
	
	public Workspace getWorkspace() {
		return workspace;
	}
	
	public ILogger getLogger() {
		return logger;
	}
	
	public ISpace findSpaceById(long id) {
		return spaceIdToSpace.get(id);
	}
	
	public synchronized Set<ISpace> getAllSpaces() {
		return Collections.unmodifiableSet(allSpaces);
	}
	
	public synchronized Set<ISpace> getOpenSpaces() {
		return Collections.unmodifiableSet(openSpaces);
	}
	
	public synchronized ISpace findSpaceForTaskId(long taskId) {
		return taskIdToSpace.get(taskId);
	}
	
	public synchronized void notifySpaceChange(ISpace space) {
		fireSpaceChangeEvent(space);
	}

	synchronized void notifySpaceContentChange(ISpaceContentChangeEvent event) {
		getEventManager().fireEvent(event);
	}

	public synchronized void addEntityToSpace(IEntity entity, long spaceId) {
		ISpace space = spaceIdToSpace.get(spaceId);
		space.addEntity(entity); // events will be fired on this call
	}
	
	public synchronized void addTaskToSpace(long taskId, ISpace space) {
		taskIdToSpace.put(taskId, space);
		database.store(taskIdToSpace);
		fireSpaceChangeEvent(space);
	}
	
	synchronized void openSpace(ISpace space) {		
		openSpaces.add(space);
		getEventManager().fireEvent(new SpaceOpenCloseEvent(space, true));
	}
	
	synchronized void closeSpace(ISpace space) {
		openSpaces.remove(space);
		getEventManager().fireEvent(new SpaceOpenCloseEvent(space, false));
	}
	
	public synchronized ISpace createSpace(IEntity root, IProbe probe) {
		final long id = generateNewSpaceId();
		if (root instanceof SpaceEntity) {
			SpaceEntity spaceEntity = (SpaceEntity)root;
			if (spaceEntity.getSpaceId() == -1) {
				spaceEntity.setSpaceId(id);
				spaceEntity.save();
			}
		}
		final ISpace space = new Space(id, probe, "Space " + id, root, this);
		database.store(space);
		allSpaces.add(space);
		spaceIdToSpace.put(space.getId(), space);
		database.store(allSpaces);
		database.store(spaceIdToSpace);
		commit();
		getEventManager().fireEvent(new SpaceCreateEvent(space)); 
		return space;
	}
	
	private synchronized long generateNewSpaceId() {
		currentSpaceId += 1;
		commit();
		return currentSpaceId;
	}
	
	private void checkCanDeleteSpace(ISpace space) {
		// dont delete spaces with running tasks
		if (space.isActive()) {
			throw new RuntimeException("Space '"+space.getName()+"' can't be deleted because it is active running tasks");
		}
		
		// if isolated, check if can delete subspaces first
		if (space.isIsolated()) {
			for (ISpace subspace: allSpaces) {
				if (subspace != space && subspace.getRootEntity() == space.getRootEntity())
					checkCanDeleteSpace(subspace);
			}
		}
	}
	
	synchronized void deleteSpace(ISpace space) {
		checkCanDeleteSpace(space);

		// if isolated, delete subspaces first
		if (space.isIsolated()) {
			for (ISpace subspace: allSpaces) {
				if (subspace != space && subspace.getRootEntity() == space.getRootEntity())
					subspace.delete();
			}
		}

		// close the space and subsequently close any open editors on it
		space.close();
		
		allSpaces.remove(space);
		spaceIdToSpace.remove(space.getId());
		for (ITaskRecord task: space.getTasks()) {
			taskIdToSpace.remove(task.getTaskId());
		}
		if (space.isIsolated()) {
			((AbstractEntity)space.getRootEntity()).delete();
		}
		database.store(allSpaces);
		database.store(spaceIdToSpace);
		database.store(taskIdToSpace);
		database.delete(space);
		commit();
		getEventManager().fireEvent(new SpaceDeleteEvent(space)); 
	}
	
	private void commit() {	
		database.store(this);
	}
	
	private void fireSpaceChangeEvent(ISpace space) {
		getEventManager().fireEvent(new SpaceChangeEvent(space));
	}

	public void addChangeListener(IEventHandler handler) {
		getEventManager().addListener(handler);
	}
	
	public void removeChangeListener(IEventHandler handler) {
		getEventManager().removeListener(handler);
	}
	
	private EventListenerManager getEventManager() {
		if(changeListeners == null) {
			changeListeners = new EventListenerManager();
		}
		return changeListeners;
	}
}
