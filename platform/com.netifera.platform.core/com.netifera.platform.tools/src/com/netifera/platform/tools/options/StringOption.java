package com.netifera.platform.tools.options;

import com.netifera.platform.api.tools.IParsableOption;

public class StringOption extends Option implements IParsableOption {
	private static final long serialVersionUID = -9151568925045179643L;
	
	String value;
	String defaultValue;
	String[] possibleValues;
	boolean allowEmpty;

	public StringOption(String name, String label, String description, String value) {
		this(name, label, description, value, false);
	}

	public StringOption(String name, String label, String description, String[] possibleValues) {
		this(name, label, description, possibleValues.length>0 ? possibleValues[0] : "", possibleValues, false);
	}

	public StringOption(String name, String label, String description, String section, String value) {
		this(name, label, description, value);
		setSection(section);
	}

	public StringOption(String name, String label, String description, String value, boolean allowEmpty) {
		this(name, label, description, value, (String[]) null, allowEmpty);
	}
	
	public StringOption(String name, String label, String description, String value, String[] possibleValues, boolean allowEmpty) {
		super(name, label, description);
		this.value = value;
		this.defaultValue = value;
		this.possibleValues = possibleValues;
		this.allowEmpty = allowEmpty;
	}
	
	public StringOption(String name, String label, String description, String section, String value, boolean allowEmpty) {
		this(name, label, description, value, allowEmpty);
		setSection(section);
	}
	
	public String getValue() {
		return value;
	}
	
	public String getDefault() {
		return defaultValue;
	}
	
	public void setDefault(final String s) {
		defaultValue = s;
	}
	
	public void setValue(final String s) {
		value = s; // FIXME can corrupt?
	}
	
	@Override
	public boolean isDefault() {
		return value.equals(defaultValue);
	}
	
	@Override
	public void setToDefault() {
		value = new String(defaultValue);
	}

	public boolean allowsEmptyValue() {
		return allowEmpty;
	}
	
	public String[] getPossibleValues() {
		return possibleValues;
	}
	
	public boolean fromString(final String text) {
		value = text;
		return true;
	}
}
