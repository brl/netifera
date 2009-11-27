package com.netifera.platform.api.model;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class EntityData implements Serializable {

	private static final long serialVersionUID = -7832133888471283L;

	private long timestamp;

	private Map<String, String> attributes;
	private Map<String, IEntityReference> associations;
	private Map<String, Set<IEntityReference>> multiAssociations;
	private Set<String> tags;

	public EntityData() {
		setTimestamp();
	}

	private void setTimestamp() {
		timestamp = Calendar.getInstance().getTimeInMillis();
	}
	
	public Date getTimestamp() {
		return new Date(timestamp);
	}
	
	// Private accessors for lazy initialization

	private Map<String, String> getAttributesMap() {
		if(attributes == null) {
			attributes = new HashMap<String, String>();
		}
		return attributes;
	}

	private Map<String, IEntityReference> getAssociationsMap() {
		if(associations == null) {
			associations = new HashMap<String, IEntityReference>();
		}
		return associations;
	}

	private Map<String, Set<IEntityReference>> getMultiAssociationsMap() {
		if(multiAssociations == null) {
			multiAssociations = new HashMap<String, Set<IEntityReference>>();
		}
		return multiAssociations;
	}

	private Set<IEntityReference> getMultiAssociationsSet(String name) {
		Set<IEntityReference> set = getMultiAssociationsMap().get(name);
		if (set == null) {
			set = new HashSet<IEntityReference>();
			getMultiAssociationsMap().put(name, set);
		}
		return set;
	}

	private Set<String> getTagsSet() {
		if(tags == null) {
			tags = new HashSet<String>();
		}
		return tags;
	}

	
	// Data API
	
	public synchronized void setAttribute(final String name, final String value) {
		getAttributesMap().put(name, value);
		setTimestamp();
	}

	public synchronized String getAttribute(final String name) {
		return getAttributesMap().get(name);
	}

	public synchronized void setAssociation(String name, IEntity value) {
		getAssociationsMap().put(name, value.createReference());
		setTimestamp();
	}

	public synchronized IEntityReference getAssociation(final String name) {
		IEntityReference ref = getAssociationsMap().get(name);
		if (ref == null) {
			Set<IEntityReference> set = getMultiAssociationsMap().get(name);
			if (set != null) {
				for (IEntityReference any: set)
					return any;
			}
		}
		return ref;
	}

	public synchronized void addAssociation(String name, IEntity value) {
		getMultiAssociationsSet(name).add(value.createReference());
		setTimestamp();
	}

	public synchronized void removeAssociation(String name, IEntity value) {
		getMultiAssociationsSet(name).remove(value.createReference());
		setTimestamp();
	}

	public synchronized Set<IEntityReference> getAssociations(String name) {
		return Collections.unmodifiableSet(getMultiAssociationsMap().get(name));
	}
	
	public synchronized Set<String> getTags() {
		if (tags == null)
			return Collections.emptySet();
		else
			return Collections.unmodifiableSet(tags);
	}

	public synchronized void addTag(String tag) {
		getTagsSet().add(tag);
		setTimestamp();
	}

	public synchronized void removeTag(String tag) {
		if (tags == null) return;
		tags.remove(tag);
		setTimestamp();
	}

	public void synchronizeData(EntityData masterData) {
		this.timestamp = masterData.timestamp;
		
		if(masterData.attributes != null) {
			mergeAttributes(masterData.attributes, getAttributesMap());
		}
		
		if(masterData.associations != null) {
			mergeAssociations(masterData.associations, getAssociationsMap());	
		}
		
		if(masterData.tags != null) {
			mergeTags(masterData.tags, getTagsSet());	
		}
	}
	
	/* These merge methods replace old information with new information */
	
	private void mergeAttributes(Map<String, String> from, Map<String,String> to) {
		for(String key : from.keySet()) 
			to.put(key, from.get(key));
		//FIXME what if an attribute was removed?
	}
	
	private void mergeAssociations(Map<String, IEntityReference> from, Map<String, IEntityReference> to) {
		for(String key : from.keySet()) 
			to.put(key, from.get(key));	
		//FIXME what if an association was removed?
	}
	
	private void mergeTags(Set<String> from, Set<String> to) {
		for(String tag : from)
			to.add(tag);
		//FIXME what if an attribute was removed?
	}
}
