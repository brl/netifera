package com.netifera.platform.ui.spaces.tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.netifera.platform.api.model.IEntity;
import com.netifera.platform.api.model.IShadowEntity;
import com.netifera.platform.api.model.IStructureContext;
import com.netifera.platform.api.model.ITreeStructureContext;

public class TreeStructureContext implements ITreeStructureContext {
	final private Tree tree;
	final private IShadowEntity entity;
	private IShadowEntity parent;
	private List<IShadowEntity> children;
	
	public static IShadowEntity createRoot(Tree tree, IEntity entity) {
		return createNode(tree, entity);
	}

	private TreeStructureContext(Tree tree, IEntity entity) {
		this.tree = tree;
		this.entity = entity.shadowClone(this);
	}
	
	public IShadowEntity getEntity() {
		return entity;
	}

	public IShadowEntity getParent() {
		return parent;
	}

	public List<IShadowEntity> getChildren() {
		List<IShadowEntity> retChildren;
		
		if(children == null) {
			return Collections.emptyList();
		}
		/*return a copy to avoid concurrent modification exceptions */
		synchronized(children) {
			retChildren = new ArrayList<IShadowEntity>(children); 
		}
		return retChildren;
	}

	public Tree getStructure() {
		return tree;
	}

	public IShadowEntity addChild(IEntity entity) {
		synchronized(this) {
			if(children == null) {
				children = Collections.synchronizedList(new LinkedList<IShadowEntity>());
			}
		}
		
		IShadowEntity shadow = createNode(tree, entity, this.entity);
		children.add(shadow);
		return shadow;
	}
	
	public IShadowEntity removeChild(IEntity entity) {
		if (children == null) return null;
		synchronized(children) {
			return removeChildEntity(entity);
		}	
	}
	
	private IShadowEntity removeChildEntity(IEntity entity) {
		for (int i=0; i<children.size(); i++) {
			if (children.get(i).getRealEntity() == entity) {
				IShadowEntity shadow = children.remove(i);
				shadow.dispose();
				return shadow;
			}
		}
		return null;
	}

	public boolean hasChildren() {
		return (children != null && children.size() > 0);
	}
	
	public boolean hasChild(IEntity entity) {
		return getChild(entity) != null;
	}

	private IShadowEntity getChild(IEntity entity) {
		if (children == null) {
			return null;
		} else synchronized(children) {
			return getChildById(entity.getId());
		}
	}
	
	private IShadowEntity getChildById(long id) {
		for (IShadowEntity child: children)
			if (child.getId() == id)
				return child;
		return null;
	}

	public boolean isRoot() {
		return parent == null;
	}
	
	public IShadowEntity searchEntityById(final long entityId) {
		if(entity.getId() == entityId) return entity;
		
		if(children == null) {
			return null;
		} else synchronized(children) {
			return searchChildrenById(entityId);
		}
	}
	
	private IShadowEntity searchChildrenById(long entityId) {
		for(IShadowEntity child: children) {
			IStructureContext ctx = child.getStructureContext();
			IShadowEntity found = ctx.searchEntityById(entityId);
			if(found != null) return found;
		}
		return null;
	}
	
	public IShadowEntity searchEntity(IEntity target) {
		if(entity.getRealEntity() == target) return entity;
		
		if(children == null) {
			return null;
		} else synchronized(children) {
			return searchChildren(target);
		}
	}
	
	private IShadowEntity searchChildren(IEntity target) {
		for(IShadowEntity child: children) {
			IShadowEntity found = child.searchEntity(target);
			if(found != null) return found;
		}
		return null;
	}

	public void dispose() {
		if (children == null)
			return;
		for(IShadowEntity child: children) {
			child.dispose();
		}
		children = null;
	}
	
	private static IShadowEntity createNode(Tree tree, IEntity entity) {
		return createNode(tree, entity, null);
	}
	
	private static IShadowEntity createNode(Tree tree, IEntity entity, IShadowEntity parent) {
		TreeStructureContext context = new TreeStructureContext(tree, entity);
		context.parent = parent;
		return context.getEntity();
	}
}
