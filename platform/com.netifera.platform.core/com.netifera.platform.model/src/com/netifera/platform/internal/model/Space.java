package com.netifera.platform.internal.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.db4o.ObjectContainer;
import com.db4o.ext.DatabaseClosedException;
import com.netifera.platform.api.events.EventListenerManager;
import com.netifera.platform.api.events.IEventHandler;
import com.netifera.platform.api.model.IEntity;
import com.netifera.platform.api.model.ISpace;
import com.netifera.platform.api.model.IWorkspace;
import com.netifera.platform.api.probe.IProbe;
import com.netifera.platform.api.tasks.ITaskRecord;
import com.netifera.platform.api.tasks.ITaskStatus;
import com.netifera.platform.internal.model.events.SpaceActivityEvent;
import com.netifera.platform.internal.model.events.SpaceContentChangeEvent;
import com.netifera.platform.internal.model.events.SpaceDeleteEvent;
import com.netifera.platform.internal.model.events.SpaceOpenCloseEvent;
import com.netifera.platform.internal.model.events.SpaceRenameEvent;
import com.netifera.platform.internal.model.events.SpaceTaskChangeEvent;
import com.netifera.platform.model.ProbeEntity;
import com.netifera.platform.model.SpaceEntity;

public class Space implements ISpace {
	private final static int BACKGROUND_COMMIT_INTERVAL = 5000;
	
	/* Unique ID value for this space */
	private final long id;
	
	/* Name for this space to display in the user interface */
	private String name;
	
	/* Every space has a root entity which is a realm creating entity.  Generally this is a probe entity */
	private final IEntity rootEntity;
	
	/* Every space is permanently associated with a single Probe entity */
	private final ProbeEntity probeEntity;
	
	/* The list of entities that are contained in this space. */
	private final List<IEntity> entities;
	
	/* The list of tasks which have been executed in this space */
	private final List<ITaskRecord> tasks;
	private transient Set<ITaskRecord> activeTasks;
	
	private final SpaceManager manager;
	private transient boolean isOpened;
	
	/* Optimization to quickly test if an entity is present in this view */
	private transient Set<IEntity> entitySet;

	private transient EventListenerManager spaceChangeListeners;
	
	private transient EventListenerManager taskChangeListeners;
	
	private transient volatile Thread commitThread;
	private transient volatile boolean entitiesDirty;
	private transient volatile boolean tasksDirty;
	private transient ObjectContainer database;
	
	/* Create a new space */
	Space(long id, IProbe probe, String name, IEntity root, SpaceManager manager) {
		this.id = id;
		this.probeEntity = (ProbeEntity) probe.getEntity();
		this.name = name;
		this.rootEntity = root;
		this.entities = Collections.synchronizedList(new ArrayList<IEntity>());
		this.tasks = Collections.synchronizedList(new ArrayList<ITaskRecord>());
		this.activeTasks = Collections.synchronizedSet(new HashSet<ITaskRecord>());
		this.entitySet = Collections.synchronizedSet(new HashSet<IEntity>());
		this.manager = manager;
		this.database = manager.getDatabase();
	}
	
	public void objectOnActivate(ObjectContainer container) {
		this.database = container;
		buildEntitySet();
		buildActiveTasksSet();
	}

	ObjectContainer getDatabase() {
		return database;
	}

	public boolean isOpened() {
		return isOpened;
	}
	
	public void open() {
		if (!isOpened) {
			isOpened = true;
			manager.openSpace(this);
			startCommitThread();
		}
	}
	
	public void close() {
		if (isOpened) {
			//FIXME should not close active spaces?
			stopCommitThread();
			isOpened = false;
			manager.closeSpace(this);
			getEventManager().fireEvent(new SpaceOpenCloseEvent(this, false));
		}
	}

	public void delete() {
		manager.deleteSpace(this);
		getEventManager().fireEvent(new SpaceDeleteEvent(this));
	}
	
	public List<IEntity> getEntities() {
		return Collections.unmodifiableList(entities);
	}

	public int entityCount() {
		return entities.size();
	}

	public boolean contains(IEntity entity) {
		return entitySet.contains(entity);
	}
	
	public void addEntity(IEntity entity) {
		if (!entitySet.contains(entity)) {
			open();
			entitySet.add(entity);
			entities.add(entity);
			entitiesDirty = true;
			SpaceContentChangeEvent event = SpaceContentChangeEvent.createAddEvent(this, entity);
			getEventManager().fireEvent(event);
			manager.notifySpaceChange(this);
			Thread.yield();
		}
	}
	
	public void updateEntity(IEntity entity) {
		if(entitySet.contains(entity)) {
			getEventManager().fireEvent(SpaceContentChangeEvent.createUpdateEvent(this, entity));
			Thread.yield();
		}
	}
	
	public void removeEntity(IEntity entity) {
		if (entitySet.contains(entity)) {
			open();
			entitySet.remove(entity);
			entities.remove(entity);
			entitiesDirty = true;
			SpaceContentChangeEvent event = SpaceContentChangeEvent.createRemoveEvent(this, entity);
			getEventManager().fireEvent(event);
			manager.notifySpaceChange(this);
			Thread.yield();
		}
	}

	public Iterator<IEntity> iterator() {
		return new Iterator<IEntity>() {
			int index = 0;
			IEntity nextEntity = nextEntity();

			private IEntity nextEntity() {
				synchronized(entities) {
					if (index < entities.size()) {
						index += 1;
						return entities.get(index-1);
					} else {
						return null;
					}
				}
			}
			
			public boolean hasNext() {
				return nextEntity != null;
			}
	
			public IEntity next() {
				IEntity answer = nextEntity;
				nextEntity = nextEntity();
				return answer;
			}

			public void remove() {
				throw new UnsupportedOperationException("Remove not supported in Space iterators");
			}
		};
	}

	public Set<String> getTags() {
		Set<String> tags = new HashSet<String>();
		synchronized(entitySet) {
			for (IEntity entity: entitySet) {
				tags.addAll(entity.getTags());
			}
		}
		return tags;
	}
	
	public void addTask(ITaskStatus status) {
		open();
		TaskRecord record = new TaskRecord(status, this);
		if(!tasks.contains(record)) {
			database.store(record); //XXX ?????
			tasks.add(record);
			updateActiveTasks(record);
			manager.addTaskToSpace(status.getTaskId(), this);
			tasksDirty = true;
			getTaskEventManager().fireEvent(SpaceTaskChangeEvent.createCreationEvent(record));
		}
	}
	
	public void updateTask(ITaskRecord record) {
		open();
		updateActiveTasks(record);
		getTaskEventManager().fireEvent(SpaceTaskChangeEvent.createUpdateEvent(record));
	}

	private void updateActiveTasks(ITaskRecord record) {
		if (record.isRunning() ? activeTasks.add(record) : activeTasks.remove(record)) {
			getEventManager().fireEvent(new SpaceActivityEvent(this, isActive()));
		}
	}
	
	public ITaskRecord[] getTasks() {
		return tasks.toArray(new ITaskRecord[tasks.size()]);
	}
	
	public boolean isActive() {
		return activeTasks.size() > 0;
	}
	
	private void buildEntitySet() {
		entitySet = Collections.synchronizedSet(new HashSet<IEntity>());
		synchronized(entities) {
			for (IEntity entity: entities) {
				entitySet.add(entity);
			}
		}
	}

	private void buildActiveTasksSet() {
		activeTasks = Collections.synchronizedSet(new HashSet<ITaskRecord>());
		synchronized(tasks) {
			for (ITaskRecord task: tasks) {
				if (task.isRunning())
					activeTasks.add(task);
			}
		}
	}
	
	public IWorkspace getWorkspace() {
		return rootEntity.getWorkspace();
	}
	
	public IEntity getRootEntity() {
		return rootEntity;
	}
	
	public boolean isIsolated() {
		return (rootEntity instanceof SpaceEntity) && (((SpaceEntity)rootEntity).getSpaceId() == id);
	}
	
	public long getId() {
		return id;
	}

	public long getProbeId() {
		return probeEntity.getProbeId();
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
		database.store(this);
		manager.notifySpaceChange(this);
		getEventManager().fireEvent(new SpaceRenameEvent(this));
	}

	public void addChangeListenerAndPopulate(final IEventHandler handler) {
		addChangeListener(handler);
		
		for(IEntity entity: this) {
			handler.handleEvent(SpaceContentChangeEvent.createAddEvent(Space.this, entity));
			if (Thread.currentThread().isInterrupted()) {
				return;
			}
		}
	}
	
	public void addChangeListener(IEventHandler handler) {
		getEventManager().addListener(handler);		
	}

	public void removeChangeListener(IEventHandler handler) {
		getEventManager().removeListener(handler);		
	}
	
	private EventListenerManager getEventManager() {
		if(spaceChangeListeners == null) {
			spaceChangeListeners = new EventListenerManager();
		}
		return spaceChangeListeners;
	}
	
	public void addTaskChangeListenerAndPopulate(IEventHandler handler) {
		getTaskEventManager().addListener(handler);
		synchronized(tasks) {
			for(ITaskRecord task : tasks) {
				handler.handleEvent(SpaceTaskChangeEvent.createCreationEvent(task));
			}
		}
	}
	
	public void addTaskChangeListener(IEventHandler handler) {
		getTaskEventManager().addListener(handler);
	}
	
	public void removeTaskChangeListener(IEventHandler handler) {
		getTaskEventManager().removeListener(handler);
	}
	
	private EventListenerManager getTaskEventManager() {
		if(taskChangeListeners == null) {
			taskChangeListeners = new EventListenerManager();
		}
		return taskChangeListeners;
	}
	
	public String toString() {
		return "Space (" + name + ") " + (isOpened ? "[opened]" : "[closed]");
	}
	
	public boolean equals(Object other) {
		if(!(other instanceof Space)) {
			return false;
		}
		return ((Space)other).id == id;
	}
	
	public int hashCode() {
		return (int) id;
	}
	
	private synchronized void startCommitThread() {
		if (commitThread != null)
			return;
		commitThread = new Thread(new Runnable() {
			public void run() {
//				System.out.println("started "+Space.this);
				try {
					while(true) {
						try {
							Thread.sleep(BACKGROUND_COMMIT_INTERVAL);
							if(database.ext().isClosed())
								return;
							commit();
						} catch (InterruptedException e) {
							commit(); // one last commit
							Thread.currentThread().interrupt();
							return;
						} catch (DatabaseClosedException e) {
							return;
						}
					}
				} finally {
//					System.out.println("stopped "+Space.this);
					commitThread = null;
				}
			}
		});
		commitThread.setDaemon(true);
		commitThread.setName("Background Commit thread for space [" + name + "]");
		commitThread.start();
	}

	private synchronized void stopCommitThread() {
		Thread nonVolatileCommitThread = commitThread;
		if (nonVolatileCommitThread == null)
			return;
		nonVolatileCommitThread.interrupt();
	}
	
	private synchronized void commit() {		
		if(tasksDirty) {
			commitTasks();
			tasksDirty = false;
		}
		
		if(entitiesDirty) {
			commitEntities();
			entitiesDirty = false;
		}
	}
	
	private void commitEntities() {
		synchronized(entities) {
			database.store(entities);
		}
	}
	
	private void commitTasks() {
		synchronized (tasks) {
			database.store(tasks);
		}
	}
}
