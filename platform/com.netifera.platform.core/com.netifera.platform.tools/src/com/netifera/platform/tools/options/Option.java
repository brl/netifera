package com.netifera.platform.tools.options;

import java.io.Serializable;

import com.netifera.platform.api.tools.IOption;

public abstract class Option implements Serializable, IOption {
	private static final long serialVersionUID = 7040773117418902642L;
	
	private String name;
	private String label;
	private String description;
	private String section;
	
	boolean fixed = false;
	
	protected Option() {}
	
	protected Option(String name, String label, String description) {
		this.name = name;
		this.label = label;
		this.description = description;
	}
	
	public String getName() {
		return name;
	}
	
	public String getLabel() {
		return label;
	}

	public String getDescription() {
		return description;
	}
	
	public String getSection() {
		return section;
	}
	
	public void setSection(String section) {
		this.section = section;
	}
	
	public boolean isFixed() {
		return fixed;
	}
	
	public void fix() {
		fixed = true;
	}

	@Override
	public String toString() {
		return name+": "+getValue();
	}
	
	abstract public boolean isDefault();
	abstract public void setToDefault();
}
